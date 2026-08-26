package com.example.authapp.service;

import com.example.authapp.dto.CreateRequestDTO;
import com.example.authapp.dto.RequestDetailDTO;
import com.example.authapp.dto.RequestSummaryDTO;
import com.example.authapp.entity.*;
import com.example.authapp.exception.WorkflowException;
import com.example.authapp.repository.*;
import com.example.authapp.workflow.engine.WorkflowEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RequestService}, the layer that owns "is this a valid
 * request type", "can this user see / cancel this request", and the mapping
 * to the view DTOs. The workflow engine is mocked, so these tests are about
 * request validation and authorization, not workflow transitions.
 *
 * <p>Kept as flat test methods (rather than {@code @Nested} groups) so mock
 * injection behaves identically across Mockito versions.
 */
@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

    @Mock private WorkflowDefinitionRepository workflowDefinitionRepository;
    @Mock private WorkflowInstanceRepository workflowInstanceRepository;
    @Mock private WorkflowTaskRepository workflowTaskRepository;
    @Mock private WorkflowHistoryRepository workflowHistoryRepository;
    @Mock private RequestRepository requestRepository;
    @Mock private WorkflowEngine workflowEngine;

    @InjectMocks private RequestService service;

    // ---------------------------------------------------------------------
    // createRequest() — Request Management: create + type validation
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("createRequest(): a valid request type starts a workflow and persists the request")
    void validTypeStartsWorkflow() {
        User employee = employee(1L, "karan");
        CreateRequestDTO dto = new CreateRequestDTO();
        dto.setRequestType("LEAVE_REQUEST");
        dto.setTitle("Leave: Sep 10-12");
        dto.setPriority(Priority.HIGH);

        WorkflowDefinition def = leaveDefinition();
        WorkflowInstance instance = instanceAt(def, employee, WorkflowStatus.PENDING_MANAGER_APPROVAL, 1);

        when(workflowDefinitionRepository.findByWorkflowTypeAndActiveTrue("LEAVE_REQUEST"))
                .thenReturn(Optional.of(def));
        when(workflowEngine.start(def, employee)).thenReturn(instance);
        when(requestRepository.save(any(Request.class))).thenAnswer(inv -> {
            Request r = inv.getArgument(0);
            r.setId(5L);
            return r;
        });

        RequestSummaryDTO summary = service.createRequest(dto, employee);

        assertThat(summary.getRequestId()).isEqualTo(5L);
        assertThat(summary.getWorkflowInstanceId()).isEqualTo(100L);
        assertThat(summary.getRequestType()).isEqualTo("LEAVE_REQUEST");
        assertThat(summary.getTitle()).isEqualTo("Leave: Sep 10-12");
        assertThat(summary.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(summary.getStatus()).isEqualTo(WorkflowStatus.PENDING_MANAGER_APPROVAL);
        assertThat(summary.getCurrentStepName()).isEqualTo("Manager Approval");

        verify(workflowEngine).start(def, employee);
        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(requestRepository).save(requestCaptor.capture());
        Request saved = requestCaptor.getValue();
        assertThat(saved.getRequestType()).isEqualTo("LEAVE_REQUEST");
        assertThat(saved.getWorkflowInstance()).isSameAs(instance);
    }

    @Test
    @DisplayName("createRequest(): an unknown request type is rejected before any workflow starts")
    void unknownTypeThrowsAndStartsNothing() {
        User employee = employee(1L, "karan");
        CreateRequestDTO dto = new CreateRequestDTO();
        dto.setRequestType("TELEPORTATION_REQUEST");
        dto.setTitle("Beam me up");

        when(workflowDefinitionRepository.findByWorkflowTypeAndActiveTrue("TELEPORTATION_REQUEST"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createRequest(dto, employee))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("not a recognized");

        verifyNoInteractions(workflowEngine);
        verify(requestRepository, never()).save(any());
    }

    // ---------------------------------------------------------------------
    // cancelRequest() — authorization then delegate to the engine
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("cancelRequest(): the owner may cancel their own request")
    void ownerCanCancel() {
        User owner = employee(1L, "karan");
        WorkflowInstance instance = instanceAt(leaveDefinition(), owner,
                WorkflowStatus.PENDING_MANAGER_APPROVAL, 1);
        Request request = request(5L, instance, "LEAVE_REQUEST", "Leave");

        when(requestRepository.findById(5L)).thenReturn(Optional.of(request));

        service.cancelRequest(5L, owner);

        verify(workflowEngine).cancel(instance, owner);
    }

    @Test
    @DisplayName("cancelRequest(): an admin may cancel someone else's request")
    void adminCanCancelOthers() {
        User owner = employee(1L, "karan");
        User admin = withRole(9L, "root", ERole.ROLE_ADMIN);
        WorkflowInstance instance = instanceAt(leaveDefinition(), owner,
                WorkflowStatus.PENDING_MANAGER_APPROVAL, 1);
        Request request = request(5L, instance, "LEAVE_REQUEST", "Leave");

        when(requestRepository.findById(5L)).thenReturn(Optional.of(request));

        service.cancelRequest(5L, admin);

        verify(workflowEngine).cancel(instance, admin);
    }

    @Test
    @DisplayName("cancelRequest(): a stranger who is neither owner nor admin is refused (unauthorized access)")
    void strangerIsRefused() {
        User owner = employee(1L, "karan");
        User stranger = employee(7L, "mallory");
        WorkflowInstance instance = instanceAt(leaveDefinition(), owner,
                WorkflowStatus.PENDING_MANAGER_APPROVAL, 1);
        Request request = request(5L, instance, "LEAVE_REQUEST", "Leave");

        when(requestRepository.findById(5L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.cancelRequest(5L, stranger))
                .isInstanceOf(AccessDeniedException.class);

        verify(workflowEngine, never()).cancel(any(), any());
    }

    @Test
    @DisplayName("cancelRequest(): cancelling a non-existent request surfaces a clear error")
    void missingRequestThrows() {
        when(requestRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelRequest(404L, employee(1L, "karan")))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("not found");
    }

    // ---------------------------------------------------------------------
    // getRequestDetail() — view detail + timeline, with access control
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("getRequestDetail(): the owner sees the request with its full history timeline")
    void ownerSeesTimeline() {
        User owner = employee(1L, "karan");
        WorkflowInstance instance = instanceAt(leaveDefinition(), owner,
                WorkflowStatus.PENDING_MANAGER_APPROVAL, 1);
        Request request = request(5L, instance, "LEAVE_REQUEST", "Leave");

        when(requestRepository.findById(5L)).thenReturn(Optional.of(request));
        when(workflowTaskRepository.findByWorkflowInstanceOrderByCreatedAtAsc(instance))
                .thenReturn(List.of());
        WorkflowHistory submitted = new WorkflowHistory(instance, "Request submitted", owner,
                null, WorkflowStatus.SUBMITTED, null);
        WorkflowHistory assigned = new WorkflowHistory(instance, "Assigned to Manager Approval (meena)", owner,
                WorkflowStatus.SUBMITTED, WorkflowStatus.PENDING_MANAGER_APPROVAL, null);
        when(workflowHistoryRepository.findByWorkflowInstanceOrderByCreatedAtAsc(instance))
                .thenReturn(List.of(submitted, assigned));

        RequestDetailDTO detail = service.getRequestDetail(5L, owner);

        assertThat(detail.getRequestId()).isEqualTo(5L);
        assertThat(detail.getStatus()).isEqualTo(WorkflowStatus.PENDING_MANAGER_APPROVAL);
        assertThat(detail.getSteps()).isNotEmpty();
        assertThat(detail.getHistory()).hasSize(2);
        assertThat(detail.getHistory().get(0).action()).isEqualTo("Request submitted");
        assertThat(detail.getHistory().get(1).newStatus()).isEqualTo("PENDING_MANAGER_APPROVAL");
    }

    @Test
    @DisplayName("getRequestDetail(): someone unrelated to the request cannot view it (unauthorized access)")
    void strangerCannotView() {
        User owner = employee(1L, "karan");
        User stranger = employee(7L, "mallory");
        WorkflowInstance instance = instanceAt(leaveDefinition(), owner,
                WorkflowStatus.PENDING_MANAGER_APPROVAL, 1);
        Request request = request(5L, instance, "LEAVE_REQUEST", "Leave");

        when(requestRepository.findById(5L)).thenReturn(Optional.of(request));
        when(workflowTaskRepository.findByWorkflowInstanceOrderByCreatedAtAsc(instance))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.getRequestDetail(5L, stranger))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ---------------------------------------------------------------------
    // listRequestsFor() — "view own" for employees, "view all" for admins
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("listRequestsFor(): an employee sees only their own requests")
    void employeeSeesOwn() {
        User employee = employee(1L, "karan");
        WorkflowInstance instance = instanceAt(leaveDefinition(), employee,
                WorkflowStatus.PENDING_MANAGER_APPROVAL, 1);
        Request request = request(5L, instance, "LEAVE_REQUEST", "Leave");

        when(workflowInstanceRepository.findByCreatedByOrderByCreatedAtDesc(employee))
                .thenReturn(List.of(instance));
        when(requestRepository.findByWorkflowInstance(instance)).thenReturn(Optional.of(request));

        List<RequestSummaryDTO> result = service.listRequestsFor(employee);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Leave");
        verify(workflowInstanceRepository, never()).findAllByOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("listRequestsFor(): an admin sees every request in the system")
    void adminSeesAll() {
        User admin = withRole(9L, "root", ERole.ROLE_ADMIN);
        User employee = employee(1L, "karan");
        WorkflowInstance instance = instanceAt(leaveDefinition(), employee,
                WorkflowStatus.PENDING_MANAGER_APPROVAL, 1);
        Request request = request(5L, instance, "LEAVE_REQUEST", "Leave");

        when(workflowInstanceRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(instance));
        when(requestRepository.findByWorkflowInstance(instance)).thenReturn(Optional.of(request));

        List<RequestSummaryDTO> result = service.listRequestsFor(admin);

        assertThat(result).hasSize(1);
        verify(workflowInstanceRepository, never()).findByCreatedByOrderByCreatedAtDesc(any());
    }

    // ---------------------------------------------------------------------
    // Fixtures
    // ---------------------------------------------------------------------

    private static User employee(Long id, String username) {
        return withRole(id, username, ERole.ROLE_EMPLOYEE);
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

    private static WorkflowInstance instanceAt(WorkflowDefinition def, User createdBy,
                                               WorkflowStatus status, int currentStep) {
        WorkflowInstance instance = new WorkflowInstance(def, createdBy);
        instance.setId(100L);
        instance.setStatus(status);
        instance.setCurrentStep(currentStep);
        return instance;
    }

    private static Request request(Long id, WorkflowInstance instance, String type, String title) {
        Request request = new Request();
        request.setId(id);
        request.setWorkflowInstance(instance);
        request.setRequestType(type);
        request.setTitle(title);
        request.setPriority(Priority.MEDIUM);
        return request;
    }
}
