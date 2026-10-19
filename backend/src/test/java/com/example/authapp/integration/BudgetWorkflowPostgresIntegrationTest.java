package com.example.authapp.integration;

import com.example.authapp.dto.CreateRequestDTO;
import com.example.authapp.dto.ProvideInformationDTO;
import com.example.authapp.dto.RequestDetailDTO;
import com.example.authapp.dto.RequestSummaryDTO;
import com.example.authapp.entity.*;
import com.example.authapp.exception.WorkflowException;
import com.example.authapp.repository.*;
import com.example.authapp.service.RequestService;
import com.example.authapp.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end integration test running against the PostgreSQL database (FlowZenix_DB).
 * Tests:
 *  - Flyway migrations and schema integrity.
 *  - JSONB metadata storage and deserialization on Request entity.
 *  - Role-based permissions (EMPLOYEE, MANAGER, FINANCE_MANAGER, ADMIN).
 *  - Separation of duties (cannot approve own request, non-assignee rejection).
 *  - Request lifecycle: Submit -> Info Requested -> Info Provided -> Manager Approved -> Finance Approved -> Completed.
 *  - In-flight vs terminal request constraints.
 */
@SpringBootTest
@Transactional
class BudgetWorkflowPostgresIntegrationTest {

    @Autowired private RequestService requestService;
    @Autowired private TaskService taskService;
    @Autowired private UserRepository userRepository;
    @Autowired private RequestRepository requestRepository;
    @Autowired private WorkflowDefinitionRepository workflowDefinitionRepository;
    @Autowired private WorkflowInstanceRepository workflowInstanceRepository;
    @Autowired private WorkflowTaskRepository workflowTaskRepository;
    @Autowired private WorkflowHistoryRepository workflowHistoryRepository;

    private User employee;
    private User manager;
    private User financeManager;
    private User admin;
    private WorkflowDefinition budgetWorkflow;

    @BeforeEach
    void setUp() {
        String uniqueSuffix = "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 5);

        // 1. Create Finance Manager
        financeManager = new User("finmgr" + uniqueSuffix, "fin" + uniqueSuffix + "@example.com", "hash");
        financeManager.setRoles(Set.of(ERole.ROLE_FINANCE_MANAGER, ERole.ROLE_FINANCE));
        financeManager.setEnabled(true);
        financeManager.setFinanceManagerReferenceId(System.currentTimeMillis() % 1000000 + 100);
        financeManager = userRepository.save(financeManager);

        // 2. Create Reporting Manager delegated to Finance Manager
        manager = new User("mgr" + uniqueSuffix, "mgr" + uniqueSuffix + "@example.com", "hash");
        manager.setRoles(Set.of(ERole.ROLE_MANAGER));
        manager.setEnabled(true);
        manager.setManagerReferenceId(System.currentTimeMillis() % 1000000 + 200);
        manager.setFinanceManagerId(financeManager.getId());
        manager = userRepository.save(manager);

        // 3. Create Employee reporting to Manager
        employee = new User("emp" + uniqueSuffix, "emp" + uniqueSuffix + "@example.com", "hash");
        employee.setRoles(Set.of(ERole.ROLE_EMPLOYEE));
        employee.setEnabled(true);
        employee.setManagerId(manager.getId());
        employee = userRepository.save(employee);

        // 4. Create Admin
        admin = new User("admin" + uniqueSuffix, "admin" + uniqueSuffix + "@example.com", "hash");
        admin.setRoles(Set.of(ERole.ROLE_ADMIN));
        admin.setEnabled(true);
        admin = userRepository.save(admin);

        // 5. Ensure active Budget Request workflow definition exists with 2 steps
        String wfType = "INTEG_BUDGET_" + uniqueSuffix;
        budgetWorkflow = new WorkflowDefinition("Integration Budget Test", "2-step approval", wfType);
        budgetWorkflow.setVersion(1);
        budgetWorkflow.setActive(true);
        budgetWorkflow.getSteps().add(new WorkflowStep(budgetWorkflow, 1, "Manager Review",
                StepType.APPROVAL, ERole.ROLE_MANAGER, true));
        budgetWorkflow.getSteps().add(new WorkflowStep(budgetWorkflow, 2, "Finance Approval",
                StepType.APPROVAL, ERole.ROLE_FINANCE, true));
        budgetWorkflow = workflowDefinitionRepository.save(budgetWorkflow);
    }

    @Test
    @DisplayName("Complete budget request lifecycle: JSONB persistence, permissions, info request/resume, manager approval, finance approval, and completion")
    void fullBudgetWorkflowLifecycleWithPostgres() {
        // --- Step 1: Employee submits Budget Request with JSONB metadata ---
        CreateRequestDTO createDTO = new CreateRequestDTO();
        createDTO.setRequestType(budgetWorkflow.getWorkflowType());
        createDTO.setTitle("Q4 Server Hardware Purchase");
        createDTO.setDescription("Procure 4 rack servers for compute cluster");
        createDTO.setPriority(Priority.HIGH);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("amount", 250000);
        metadata.put("currency", "USD");
        metadata.put("vendor", "Dell Technologies");
        metadata.put("serversCount", 4);
        createDTO.setMetadata(metadata);

        RequestSummaryDTO summary = requestService.createRequest(createDTO, employee);
        assertThat(summary).isNotNull();
        assertThat(summary.getStatus()).isEqualTo(WorkflowStatus.PENDING_MANAGER_APPROVAL);

        // Verify JSONB metadata stored in PostgreSQL correctly
        Request savedRequest = requestRepository.findById(summary.getRequestId()).orElseThrow();
        assertThat(savedRequest.getMetadata())
                .isNotNull()
                .containsEntry("currency", "USD")
                .containsEntry("vendor", "Dell Technologies");
        assertThat(((Number) savedRequest.getMetadata().get("amount")).intValue()).isEqualTo(250000);

        WorkflowInstance instance = savedRequest.getWorkflowInstance();
        assertThat(instance.getCurrentStep()).isEqualTo(1);
        assertThat(instance.getStatus()).isEqualTo(WorkflowStatus.PENDING_MANAGER_APPROVAL);

        // Verify task assigned to manager
        List<WorkflowTask> tasks = workflowTaskRepository.findByWorkflowInstanceOrderByCreatedAtAsc(instance);
        assertThat(tasks).hasSize(1);
        WorkflowTask managerTask = tasks.get(0);
        assertThat(managerTask.getAssignedTo().getId()).isEqualTo(manager.getId());
        assertThat(managerTask.getStatus()).isEqualTo(TaskStatus.PENDING);

        // --- Step 2: Test Permissions & Separation of Duties ---
        // 2a. Employee cannot approve manager's task (not assigned)
        assertThatThrownBy(() -> taskService.approve(managerTask.getId(), employee, "Self-approve attempt"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not assigned to you");

        // 2b. Requester cannot approve their own request even if assigned
        managerTask.setAssignedTo(employee);
        workflowTaskRepository.save(managerTask);
        assertThatThrownBy(() -> taskService.approve(managerTask.getId(), employee, "Self-approve"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("cannot approve their own requests");

        // Restore task assignment to manager
        managerTask.setAssignedTo(manager);
        workflowTaskRepository.save(managerTask);

        // --- Step 3: Manager requests additional information ---
        taskService.requestInformation(managerTask.getId(), manager, "Please provide the formal vendor quote PDF reference");

        RequestDetailDTO detailAfterInfoReq = requestService.getRequestDetail(savedRequest.getId(), employee);
        assertThat(detailAfterInfoReq.getStatus()).isEqualTo(WorkflowStatus.PENDING_INFORMATION);

        WorkflowTask infoTask = workflowTaskRepository.findById(managerTask.getId()).orElseThrow();
        assertThat(infoTask.getStatus()).isEqualTo(TaskStatus.INFO_REQUESTED);
        assertThat(infoTask.getComment()).contains("formal vendor quote");

        // Non-owner cannot provide information
        assertThatThrownBy(() -> requestService.provideInformation(savedRequest.getId(), manager,
                new ProvideInformationDTO("Illegal response")))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only the requester");

        // --- Step 4: Employee provides requested information and resumes workflow ---
        ProvideInformationDTO infoDTO = new ProvideInformationDTO(
                "Vendor quote #DELL-2026-990 attached",
                Map.of("quoteReference", "DELL-2026-990", "quoteApprovedByLead", true)
        );
        RequestDetailDTO detailAfterResume = requestService.provideInformation(savedRequest.getId(), employee, infoDTO);
        assertThat(detailAfterResume.getStatus()).isEqualTo(WorkflowStatus.PENDING_MANAGER_APPROVAL);

        // Check metadata was merged in PostgreSQL
        Request updatedReq = requestRepository.findById(savedRequest.getId()).orElseThrow();
        assertThat(updatedReq.getMetadata()).containsEntry("quoteReference", "DELL-2026-990");
        assertThat(updatedReq.getMetadata()).containsEntry("vendor", "Dell Technologies");

        WorkflowTask resumedTask = workflowTaskRepository.findById(managerTask.getId()).orElseThrow();
        assertThat(resumedTask.getStatus()).isEqualTo(TaskStatus.PENDING);

        // --- Step 5: Manager approves -> Advances to Finance Manager ---
        taskService.approve(resumedTask.getId(), manager, "Manager approved after quote verification");

        RequestDetailDTO detailAfterMgrApprove = requestService.getRequestDetail(savedRequest.getId(), employee);
        assertThat(detailAfterMgrApprove.getStatus()).isEqualTo(WorkflowStatus.PENDING_FINANCE_APPROVAL);

        List<WorkflowTask> allTasks = workflowTaskRepository.findByWorkflowInstanceOrderByCreatedAtAsc(instance);
        assertThat(allTasks).hasSize(2);
        WorkflowTask financeTask = allTasks.get(1);
        assertThat(financeTask.getStep().getName()).isEqualTo("Finance Approval");
        assertThat(financeTask.getAssignedTo().getId()).isEqualTo(financeManager.getId());
        assertThat(financeTask.getStatus()).isEqualTo(TaskStatus.PENDING);

        // --- Step 6: Finance Manager approves -> Workflow Completes as APPROVED ---
        taskService.approve(financeTask.getId(), financeManager, "Budget approved and PO generated");

        RequestDetailDTO finalDetail = requestService.getRequestDetail(savedRequest.getId(), employee);
        assertThat(finalDetail.getStatus()).isEqualTo(WorkflowStatus.APPROVED);
        assertThat(finalDetail.getCompletedAt()).isNotNull();

        // --- Step 7: Completed request cannot be cancelled ---
        assertThatThrownBy(() -> requestService.cancelRequest(savedRequest.getId(), employee))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("APPROVED");

        // --- Step 8: Verify full audit history trail recorded in PostgreSQL ---
        List<WorkflowHistory> history = workflowHistoryRepository.findByWorkflowInstanceOrderByCreatedAtAsc(instance);
        assertThat(history).isNotEmpty();
        List<String> actions = history.stream().map(WorkflowHistory::getAction).toList();
        assertThat(actions).anyMatch(a -> a.contains("submitted"));
        assertThat(actions).anyMatch(a -> a.contains("Assigned to Manager Review"));
        assertThat(actions).anyMatch(a -> a.contains("Additional information requested"));
        assertThat(actions).anyMatch(a -> a.contains("Additional information provided"));
        assertThat(actions).anyMatch(a -> a.contains("Manager Review approved"));
        assertThat(actions).anyMatch(a -> a.contains("Assigned to Finance Approval"));
        assertThat(actions).anyMatch(a -> a.contains("Finance Approval approved"));
        assertThat(actions).anyMatch(a -> a.contains("Workflow completed"));
    }
}
