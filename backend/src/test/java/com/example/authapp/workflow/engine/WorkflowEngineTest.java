package com.example.authapp.workflow.engine;

import com.example.authapp.entity.*;
import com.example.authapp.exception.WorkflowException;
import com.example.authapp.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for the reusable {@link WorkflowEngine}. No Spring context
 * and no database — every repository is a Mockito mock, so these run in
 * milliseconds and exercise the engine's transition logic in isolation.
 *
 * <p>The engine is the single place that mutates {@code WorkflowInstance.status},
 * so it is where the Module 1 behaviours worth guarding live: starting a
 * workflow and assigning its first task, approving/rejecting/requesting-info,
 * advancing multi-step workflows, cancelling in-flight work, refusing to act
 * on already-resolved tasks, and writing a complete history trail.
 *
 * <p>Kept as flat test methods (rather than {@code @Nested} groups) so mock
 * injection behaves identically across Mockito versions.
 */
@ExtendWith(MockitoExtension.class)
class WorkflowEngineTest {

    @Mock private WorkflowInstanceRepository workflowInstanceRepository;
    @Mock private WorkflowTaskRepository workflowTaskRepository;
    @Mock private ApprovalRepository approvalRepository;
    @Mock private WorkflowHistoryRepository workflowHistoryRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private WorkflowEngine engine;

    // ---------------------------------------------------------------------
    // start() — creates the instance and the first task (Workflow Execution)
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("start(): leave request creates instance and routes the first task to the employee's manager")
    void startLeaveRequestAssignsManager() {
        User employee = employee(1L, "karan");
        employee.setManagerId(2L);
        User manager = manager(2L, "meena");
        WorkflowDefinition def = leaveDefinition();

        // start() reassigns the return of save(), so it must echo the instance back.
        when(workflowInstanceRepository.save(any(WorkflowInstance.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));

        WorkflowInstance instance = engine.start(def, employee);

        assertThat(instance.getStatus()).isEqualTo(WorkflowStatus.PENDING_MANAGER_APPROVAL);
        assertThat(instance.getCurrentStep()).isEqualTo(1);
        assertThat(instance.getCreatedBy()).isSameAs(employee);

        ArgumentCaptor<WorkflowTask> taskCaptor = ArgumentCaptor.forClass(WorkflowTask.class);
        verify(workflowTaskRepository).save(taskCaptor.capture());
        WorkflowTask firstTask = taskCaptor.getValue();
        assertThat(firstTask.getAssignedTo()).isSameAs(manager);
        assertThat(firstTask.getStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(firstTask.getStep().getName()).isEqualTo("Manager Approval");

        // History records both the submission and the assignment (Workflow History).
        ArgumentCaptor<WorkflowHistory> historyCaptor = ArgumentCaptor.forClass(WorkflowHistory.class);
        verify(workflowHistoryRepository, times(2)).save(historyCaptor.capture());
        List<String> actions = historyCaptor.getAllValues().stream().map(WorkflowHistory::getAction).toList();
        assertThat(actions.get(0)).contains("submitted");
        assertThat(actions.get(1)).contains("Assigned to Manager Approval");
    }

    @Test
    @DisplayName("start(): a definition with no steps is a configuration error, not a silent completion")
    void startWithNoStepsThrows() {
        WorkflowDefinition empty = new WorkflowDefinition("Empty", "no steps", "EMPTY");

        assertThatThrownBy(() -> engine.start(empty, employee(1L, "karan")))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("no steps");

        verifyNoInteractions(workflowTaskRepository);
    }

    @Test
    @DisplayName("start(): an employee with no manager on file cannot start a manager-routed workflow")
    void startFailsWhenEmployeeHasNoManager() {
        User employee = employee(1L, "karan"); // managerId deliberately left null
        WorkflowDefinition def = leaveDefinition();
        when(workflowInstanceRepository.save(any(WorkflowInstance.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> engine.start(def, employee))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("managerId");
    }

    // ---------------------------------------------------------------------
    // decide() — approve / reject / request-info (Approval Management)
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("decide(): approving the only step completes the workflow as APPROVED")
    void approveSingleStepCompletes() {
        User manager = manager(2L, "meena");
        WorkflowDefinition def = leaveDefinition();
        WorkflowInstance instance = instanceAt(def, employee(1L, "karan"),
                WorkflowStatus.PENDING_MANAGER_APPROVAL, 1);
        WorkflowTask task = pendingTask(instance, def.getOrderedSteps().get(0), manager);

        engine.decide(task, ApprovalDecision.APPROVED, manager, "Looks good");

        assertThat(task.getStatus()).isEqualTo(TaskStatus.APPROVED);
        assertThat(task.getComment()).isEqualTo("Looks good");
        assertThat(instance.getStatus()).isEqualTo(WorkflowStatus.APPROVED);
        assertThat(instance.getCompletedAt()).isNotNull();
        verify(approvalRepository).save(any(Approval.class));
    }

    @Test
    @DisplayName("decide(): approving step 1 of a budget request advances to Finance rather than finishing")
    void approveFirstOfTwoStepsAdvances() {
        User manager = manager(2L, "meena");
        manager.setFinanceManagerId(3L);
        User finance = withRole(3L, "fatima", ERole.ROLE_FINANCE, ERole.ROLE_FINANCE_MANAGER);
        WorkflowDefinition def = budgetDefinition();
        User employee = employee(1L, "karan");
        employee.setManagerId(2L);
        WorkflowInstance instance = instanceAt(def, employee,
                WorkflowStatus.PENDING_MANAGER_APPROVAL, 1);
        WorkflowTask step1Task = pendingTask(instance, def.getOrderedSteps().get(0), manager);

        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(userRepository.findById(3L)).thenReturn(Optional.of(finance));

        engine.decide(step1Task, ApprovalDecision.APPROVED, manager, "ok by me");

        assertThat(step1Task.getStatus()).isEqualTo(TaskStatus.APPROVED);
        assertThat(instance.getStatus()).isEqualTo(WorkflowStatus.PENDING_FINANCE_APPROVAL);
        assertThat(instance.getCurrentStep()).isEqualTo(2);
        assertThat(instance.getCompletedAt()).isNull();

        // Two task saves: the resolved step-1 task, then the freshly created finance task.
        ArgumentCaptor<WorkflowTask> taskCaptor = ArgumentCaptor.forClass(WorkflowTask.class);
        verify(workflowTaskRepository, times(2)).save(taskCaptor.capture());
        WorkflowTask financeTask = taskCaptor.getAllValues().get(1);
        assertThat(financeTask.getAssignedTo()).isSameAs(finance);
        assertThat(financeTask.getStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(financeTask.getStep().getName()).isEqualTo("Finance Approval");
    }

    @Test
    @DisplayName("decide(): a budget request cannot advance to Finance until its manager chooses a Finance Manager")
    void budgetApprovalFailsWithoutManagersFinanceAssignment() {
        User manager = manager(2L, "meena");
        User employee = employee(1L, "karan");
        employee.setManagerId(2L);
        WorkflowDefinition def = budgetDefinition();
        WorkflowInstance instance = instanceAt(def, employee,
                WorkflowStatus.PENDING_MANAGER_APPROVAL, 1);
        WorkflowTask step1Task = pendingTask(instance, def.getOrderedSteps().get(0), manager);
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));

        assertThatThrownBy(() -> engine.decide(step1Task, ApprovalDecision.APPROVED, manager, "ok"))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("has not selected a Finance Manager");
    }

    @Test
    @DisplayName("decide(): rejecting a step ends the whole workflow as REJECTED")
    void rejectEndsWorkflow() {
        User manager = manager(2L, "meena");
        WorkflowDefinition def = budgetDefinition();
        WorkflowInstance instance = instanceAt(def, employee(1L, "karan"),
                WorkflowStatus.PENDING_MANAGER_APPROVAL, 1);
        WorkflowTask task = pendingTask(instance, def.getOrderedSteps().get(0), manager);

        engine.decide(task, ApprovalDecision.REJECTED, manager, "Budget too high");

        assertThat(task.getStatus()).isEqualTo(TaskStatus.REJECTED);
        assertThat(instance.getStatus()).isEqualTo(WorkflowStatus.REJECTED);
        assertThat(instance.getCompletedAt()).isNotNull();
        // A rejection must not spin up a next-step task.
        verify(workflowTaskRepository, times(1)).save(any(WorkflowTask.class));
    }

    @Test
    @DisplayName("decide(): requesting information parks the workflow in PENDING_INFORMATION and keeps the comment")
    void requestInformationParksWorkflow() {
        User manager = manager(2L, "meena");
        WorkflowDefinition def = leaveDefinition();
        WorkflowInstance instance = instanceAt(def, employee(1L, "karan"),
                WorkflowStatus.PENDING_MANAGER_APPROVAL, 1);
        WorkflowTask task = pendingTask(instance, def.getOrderedSteps().get(0), manager);

        engine.decide(task, ApprovalDecision.INFO_REQUESTED, manager, "Which dates exactly?");

        assertThat(task.getStatus()).isEqualTo(TaskStatus.INFO_REQUESTED);
        assertThat(task.getComment()).isEqualTo("Which dates exactly?");
        assertThat(instance.getStatus()).isEqualTo(WorkflowStatus.PENDING_INFORMATION);
        assertThat(instance.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("decide(): an already-resolved task cannot be decided again (prevents duplicate approval)")
    void decideOnResolvedTaskThrows() {
        User manager = manager(2L, "meena");
        WorkflowDefinition def = leaveDefinition();
        WorkflowInstance instance = instanceAt(def, employee(1L, "karan"),
                WorkflowStatus.APPROVED, 1);
        WorkflowTask alreadyApproved = pendingTask(instance, def.getOrderedSteps().get(0), manager);
        alreadyApproved.setStatus(TaskStatus.APPROVED);

        assertThatThrownBy(() -> engine.decide(alreadyApproved, ApprovalDecision.APPROVED, manager, "again"))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("already been resolved");

        // No second approval row, no status churn.
        verifyNoInteractions(approvalRepository);
    }

    // ---------------------------------------------------------------------
    // cancel() — requester withdraws an in-flight request
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("cancel(): cancelling an in-flight request marks it CANCELLED and closes the open task")
    void cancelInFlightClosesTask() {
        User employee = employee(1L, "karan");
        User manager = manager(2L, "meena");
        WorkflowDefinition def = leaveDefinition();
        WorkflowInstance instance = instanceAt(def, employee,
                WorkflowStatus.PENDING_MANAGER_APPROVAL, 1);
        WorkflowTask openTask = pendingTask(instance, def.getOrderedSteps().get(0), manager);

        when(workflowTaskRepository.findByWorkflowInstanceAndStatus(instance, TaskStatus.PENDING))
                .thenReturn(Optional.of(openTask));

        engine.cancel(instance, employee);

        assertThat(instance.getStatus()).isEqualTo(WorkflowStatus.CANCELLED);
        assertThat(instance.getCompletedAt()).isNotNull();
        assertThat(openTask.getStatus()).isEqualTo(TaskStatus.CANCELLED);
        assertThat(openTask.getCompletedAt()).isNotNull();
        verify(workflowHistoryRepository).save(any(WorkflowHistory.class));
    }

    @Test
    @DisplayName("cancel(): an already-finished request cannot be cancelled")
    void cancelTerminalThrows() {
        WorkflowInstance approved = instanceAt(leaveDefinition(), employee(1L, "karan"),
                WorkflowStatus.APPROVED, 1);

        assertThatThrownBy(() -> engine.cancel(approved, employee(1L, "karan")))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("APPROVED");

        verifyNoInteractions(workflowTaskRepository);
        verifyNoInteractions(workflowHistoryRepository);
    }

    // ---------------------------------------------------------------------
    // End-to-end: drive a full two-step budget workflow through the engine
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("end-to-end: a budget request flows submit -> manager -> finance -> APPROVED with a full history trail")
    void endToEndBudgetLifecycle() {
        User employee = employee(1L, "karan");
        employee.setManagerId(2L);
        User manager = manager(2L, "meena");
        manager.setFinanceManagerId(3L);
        User finance = withRole(3L, "fatima", ERole.ROLE_FINANCE, ERole.ROLE_FINANCE_MANAGER);
        WorkflowDefinition def = budgetDefinition();

        // Record every task the engine saves so we can act on the latest one.
        List<WorkflowTask> savedTasks = new ArrayList<>();
        when(workflowTaskRepository.save(any(WorkflowTask.class))).thenAnswer(inv -> {
            savedTasks.add(inv.getArgument(0));
            return inv.getArgument(0);
        });
        when(workflowInstanceRepository.save(any(WorkflowInstance.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(userRepository.findById(3L)).thenReturn(Optional.of(finance));

        // 1. Submit — routes to the manager.
        WorkflowInstance instance = engine.start(def, employee);
        assertThat(instance.getStatus()).isEqualTo(WorkflowStatus.PENDING_MANAGER_APPROVAL);
        WorkflowTask managerTask = savedTasks.get(savedTasks.size() - 1);
        assertThat(managerTask.getAssignedTo()).isSameAs(manager);

        // 2. Manager approves — advances to finance.
        engine.decide(managerTask, ApprovalDecision.APPROVED, manager, "Approved by manager");
        assertThat(instance.getStatus()).isEqualTo(WorkflowStatus.PENDING_FINANCE_APPROVAL);
        WorkflowTask financeTask = savedTasks.get(savedTasks.size() - 1);
        assertThat(financeTask.getAssignedTo()).isSameAs(finance);

        // 3. Finance approves — workflow completes.
        engine.decide(financeTask, ApprovalDecision.APPROVED, finance, "Approved by finance");
        assertThat(instance.getStatus()).isEqualTo(WorkflowStatus.APPROVED);
        assertThat(instance.getCompletedAt()).isNotNull();

        // The history trail records creation, both assignments, both approvals and completion.
        ArgumentCaptor<WorkflowHistory> historyCaptor = ArgumentCaptor.forClass(WorkflowHistory.class);
        verify(workflowHistoryRepository, times(6)).save(historyCaptor.capture());
        List<String> actions = historyCaptor.getAllValues().stream().map(WorkflowHistory::getAction).toList();
        assertThat(actions).anyMatch(a -> a.contains("submitted"));
        assertThat(actions).anyMatch(a -> a.contains("Assigned to Manager Review"));
        assertThat(actions).anyMatch(a -> a.contains("Manager Review approved"));
        assertThat(actions).anyMatch(a -> a.contains("Assigned to Finance Approval"));
        assertThat(actions).anyMatch(a -> a.contains("Finance Approval approved"));
        assertThat(actions).anyMatch(a -> a.contains("completed"));
    }

    // ---------------------------------------------------------------------
    // Fixtures
    // ---------------------------------------------------------------------

    private static User employee(Long id, String username) {
        return withRole(id, username, ERole.ROLE_EMPLOYEE);
    }

    private static User manager(Long id, String username) {
        return withRole(id, username, ERole.ROLE_MANAGER);
    }

    private static User withRole(Long id, String username, ERole... roles) {
        User user = new User(username, username + "@example.com", "bcrypt-hash");
        user.setId(id);
        user.setRoles(new HashSet<>(Arrays.asList(roles)));
        return user;
    }

    private static WorkflowDefinition leaveDefinition() {
        WorkflowDefinition def = new WorkflowDefinition(
                "Employee Leave Request", "Routes to the employee's manager.", "LEAVE_REQUEST");
        def.getSteps().add(new WorkflowStep(def, 1, "Manager Approval",
                StepType.APPROVAL, ERole.ROLE_MANAGER, true));
        return def;
    }

    private static WorkflowDefinition budgetDefinition() {
        WorkflowDefinition def = new WorkflowDefinition(
                "Budget Request", "Manager then Finance.", "BUDGET_REQUEST");
        def.getSteps().add(new WorkflowStep(def, 1, "Manager Review",
                StepType.APPROVAL, ERole.ROLE_MANAGER, true));
        def.getSteps().add(new WorkflowStep(def, 2, "Finance Approval",
                StepType.APPROVAL, ERole.ROLE_FINANCE, true));
        return def;
    }

    private static WorkflowInstance instanceAt(WorkflowDefinition def, User createdBy,
                                               WorkflowStatus status, int currentStep) {
        WorkflowInstance instance = new WorkflowInstance(def, createdBy);
        instance.setId(100L);
        instance.setStatus(status);
        instance.setCurrentStep(currentStep);
        return instance;
    }

    private static WorkflowTask pendingTask(WorkflowInstance instance, WorkflowStep step, User assignee) {
        WorkflowTask task = new WorkflowTask(instance, step, assignee);
        task.setId(10L);
        task.setStatus(TaskStatus.PENDING);
        return task;
    }
}
