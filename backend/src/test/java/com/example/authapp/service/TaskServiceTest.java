package com.example.authapp.service;

import com.example.authapp.dto.TaskSummaryDTO;
import com.example.authapp.entity.*;
import com.example.authapp.exception.WorkflowException;
import com.example.authapp.repository.RequestRepository;
import com.example.authapp.repository.WorkflowTaskRepository;
import com.example.authapp.workflow.engine.WorkflowEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TaskService}, the "my approvals inbox" layer. Its job
 * is to confirm the acting user really owns the task (or is an admin) before
 * asking {@link WorkflowEngine} to run the transition. The engine is mocked,
 * so these tests focus on the ownership guard, the not-found path, the
 * decision routing (approve/reject/request-info) and the inbox mapping.
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private WorkflowTaskRepository workflowTaskRepository;
    @Mock private RequestRepository requestRepository;
    @Mock private WorkflowEngine workflowEngine;

    @InjectMocks private TaskService service;

    @Test
    @DisplayName("the assignee can approve their task, and the decision is passed to the engine")
    void assigneeCanApprove() {
        User manager = manager(2L, "meena");
        WorkflowTask task = taskAssignedTo(manager);
        when(workflowTaskRepository.findById(10L)).thenReturn(Optional.of(task));

        service.approve(10L, manager, "Approved");

        verify(workflowEngine).decide(task, ApprovalDecision.APPROVED, manager, "Approved");
    }

    @Test
    @DisplayName("reject routes a REJECTED decision to the engine")
    void assigneeCanReject() {
        User manager = manager(2L, "meena");
        WorkflowTask task = taskAssignedTo(manager);
        when(workflowTaskRepository.findById(10L)).thenReturn(Optional.of(task));

        service.reject(10L, manager, "Denied");

        verify(workflowEngine).decide(task, ApprovalDecision.REJECTED, manager, "Denied");
    }

    @Test
    @DisplayName("requestInformation routes an INFO_REQUESTED decision to the engine")
    void assigneeCanRequestInformation() {
        User manager = manager(2L, "meena");
        WorkflowTask task = taskAssignedTo(manager);
        when(workflowTaskRepository.findById(10L)).thenReturn(Optional.of(task));

        service.requestInformation(10L, manager, "Need more detail");

        verify(workflowEngine).decide(task, ApprovalDecision.INFO_REQUESTED, manager, "Need more detail");
    }

    @Test
    @DisplayName("a user who is neither the assignee nor an admin cannot act on the task (unauthorized access)")
    void nonAssigneeIsRefused() {
        User manager = manager(2L, "meena");
        User stranger = employee(7L, "mallory");
        WorkflowTask task = taskAssignedTo(manager);
        when(workflowTaskRepository.findById(10L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.approve(10L, stranger, "sneaky"))
                .isInstanceOf(AccessDeniedException.class);

        // The engine must never be reached when authorization fails.
        verifyNoInteractions(workflowEngine);
    }

    @Test
    @DisplayName("an admin may act on a task assigned to someone else")
    void adminCanActOnOthersTask() {
        User manager = manager(2L, "meena");
        User admin = withRole(9L, "root", ERole.ROLE_ADMIN);
        WorkflowTask task = taskAssignedTo(manager);
        when(workflowTaskRepository.findById(10L)).thenReturn(Optional.of(task));

        service.approve(10L, admin, "override");

        verify(workflowEngine).decide(task, ApprovalDecision.APPROVED, admin, "override");
    }

    @Test
    @DisplayName("acting on a task that does not exist surfaces a clear error")
    void missingTaskThrows() {
        when(workflowTaskRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(404L, manager(2L, "meena"), "x"))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("not found");

        verifyNoInteractions(workflowEngine);
    }

    @Test
    @DisplayName("listMyTasks maps each pending task to an inbox row")
    void listMyTasksMapsRows() {
        User employee = employee(1L, "karan");
        User manager = manager(2L, "meena");
        WorkflowDefinition def = leaveDefinition();
        WorkflowInstance instance = new WorkflowInstance(def, employee);
        instance.setId(100L);
        WorkflowTask task = new WorkflowTask(instance, def.getOrderedSteps().get(0), manager);
        task.setId(10L);
        Request request = new Request();
        request.setId(5L);
        request.setWorkflowInstance(instance);
        request.setRequestType("LEAVE_REQUEST");
        request.setTitle("Leave: Sep 10-12");

        when(workflowTaskRepository.findByAssignedToAndStatusOrderByCreatedAtDesc(manager, TaskStatus.PENDING))
                .thenReturn(List.of(task));
        when(requestRepository.findByWorkflowInstance(instance)).thenReturn(Optional.of(request));

        List<TaskSummaryDTO> rows = service.listMyTasks(manager);

        assertThat(rows).hasSize(1);
        TaskSummaryDTO row = rows.get(0);
        assertThat(row.getTaskId()).isEqualTo(10L);
        assertThat(row.getRequestId()).isEqualTo(5L);
        assertThat(row.getRequestTitle()).isEqualTo("Leave: Sep 10-12");
        assertThat(row.getRequestType()).isEqualTo("LEAVE_REQUEST");
        assertThat(row.getStepName()).isEqualTo("Manager Approval");
        assertThat(row.getRequestedBy()).isEqualTo("karan");
    }

    // ---------------------------------------------------------------------
    // Fixtures
    // ---------------------------------------------------------------------

    private static WorkflowTask taskAssignedTo(User assignee) {
        WorkflowDefinition def = leaveDefinition();
        WorkflowInstance instance = new WorkflowInstance(def, employee(1L, "karan"));
        instance.setId(100L);
        WorkflowTask task = new WorkflowTask(instance, def.getOrderedSteps().get(0), assignee);
        task.setId(10L);
        task.setStatus(TaskStatus.PENDING);
        return task;
    }

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
}
