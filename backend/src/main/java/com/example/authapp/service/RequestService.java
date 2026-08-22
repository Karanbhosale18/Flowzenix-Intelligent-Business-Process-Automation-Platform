package com.example.authapp.service;

import com.example.authapp.dto.CreateRequestDTO;
import com.example.authapp.dto.RequestDetailDTO;
import com.example.authapp.dto.RequestSummaryDTO;
import com.example.authapp.entity.*;
import com.example.authapp.exception.WorkflowException;
import com.example.authapp.repository.*;
import com.example.authapp.workflow.engine.WorkflowEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Orchestrates request creation and retrieval. Owns the "is this request
 * type valid" and "is this user allowed to see this request" decisions —
 * everything about *running* the workflow itself is delegated to
 * WorkflowEngine.
 */
@Service
public class RequestService {

    @Autowired private WorkflowDefinitionRepository workflowDefinitionRepository;
    @Autowired private WorkflowInstanceRepository workflowInstanceRepository;
    @Autowired private WorkflowTaskRepository workflowTaskRepository;
    @Autowired private WorkflowHistoryRepository workflowHistoryRepository;
    @Autowired private RequestRepository requestRepository;
    @Autowired private WorkflowEngine workflowEngine;

    @Transactional
    public RequestSummaryDTO createRequest(CreateRequestDTO dto, User submittedBy) {
        WorkflowDefinition definition = workflowDefinitionRepository
                .findByWorkflowTypeAndActiveTrue(dto.getRequestType())
                .orElseThrow(() -> new WorkflowException(
                        "'" + dto.getRequestType() + "' is not a recognized or active request type."));

        WorkflowInstance instance = workflowEngine.start(definition, submittedBy);

        Request request = new Request();
        request.setWorkflowInstance(instance);
        request.setRequestType(dto.getRequestType());
        request.setTitle(dto.getTitle());
        request.setDescription(dto.getDescription());
        request.setPriority(dto.getPriority() != null ? dto.getPriority() : Priority.MEDIUM);
        request.setMetadata(dto.getMetadata());
        request = requestRepository.save(request);

        return toSummary(request, instance);
    }

    public List<RequestSummaryDTO> listRequestsFor(User user) {
        boolean isAdmin = user.getRoles().contains(ERole.ROLE_ADMIN);
        List<WorkflowInstance> instances = isAdmin
                ? workflowInstanceRepository.findAllByOrderByCreatedAtDesc()
                : workflowInstanceRepository.findByCreatedByOrderByCreatedAtDesc(user);

        return instances.stream()
                .map(instance -> requestRepository.findByWorkflowInstance(instance)
                        .map(req -> toSummary(req, instance))
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    public RequestDetailDTO getRequestDetail(Long requestId, User currentUser) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new WorkflowException("Request " + requestId + " not found."));
        WorkflowInstance instance = request.getWorkflowInstance();

        List<WorkflowTask> tasks = workflowTaskRepository.findByWorkflowInstanceOrderByCreatedAtAsc(instance);

        boolean isOwner = instance.getCreatedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRoles().contains(ERole.ROLE_ADMIN);
        boolean isParticipant = tasks.stream().anyMatch(t -> t.getAssignedTo().getId().equals(currentUser.getId()));

        if (!isOwner && !isAdmin && !isParticipant) {
            throw new AccessDeniedException("You do not have access to this request.");
        }

        RequestDetailDTO dto = new RequestDetailDTO();
        dto.setRequestId(request.getId());
        dto.setWorkflowInstanceId(instance.getId());
        dto.setRequestType(request.getRequestType());
        dto.setTitle(request.getTitle());
        dto.setDescription(request.getDescription());
        dto.setPriority(request.getPriority());
        dto.setMetadata(request.getMetadata());
        dto.setStatus(instance.getStatus());
        dto.setCreatedAt(instance.getCreatedAt());
        dto.setCompletedAt(instance.getCompletedAt());

        List<WorkflowStep> steps = instance.getWorkflowDefinition().getOrderedSteps();
        dto.setSteps(steps.stream()
                .map(s -> new RequestDetailDTO.StepSummaryDTO(
                        s.getStepOrder(),
                        s.getName(),
                        s.getAssignedRole() != null ? s.getAssignedRole().name() : null,
                        s.getStepOrder() == instance.getCurrentStep(),
                        s.getStepOrder() < instance.getCurrentStep()
                                || (instance.getStatus() == WorkflowStatus.APPROVED && s.getStepOrder() <= instance.getCurrentStep())
                ))
                .toList());

        dto.setHistory(workflowHistoryRepository.findByWorkflowInstanceOrderByCreatedAtAsc(instance).stream()
                .map(h -> new RequestDetailDTO.HistoryEntryDTO(
                        h.getAction(),
                        h.getPerformedBy() != null ? h.getPerformedBy().getUsername() : "system",
                        h.getOldStatus() != null ? h.getOldStatus().name() : null,
                        h.getNewStatus().name(),
                        h.getComment(),
                        h.getCreatedAt()))
                .toList());

        Optional<WorkflowTask> myPending = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.PENDING)
                .filter(t -> t.getAssignedTo().getId().equals(currentUser.getId()))
                .findFirst();
        myPending.ifPresent(t -> dto.setMyPendingTaskId(t.getId()));

        return dto;
    }

    private RequestSummaryDTO toSummary(Request request, WorkflowInstance instance) {
        String currentStepName = instance.getWorkflowDefinition().getOrderedSteps().stream()
                .filter(s -> s.getStepOrder() == instance.getCurrentStep())
                .map(WorkflowStep::getName)
                .findFirst()
                .orElse(instance.getStatus().name());

        return new RequestSummaryDTO(
                request.getId(),
                instance.getId(),
                request.getRequestType(),
                request.getTitle(),
                request.getPriority(),
                instance.getStatus(),
                currentStepName,
                instance.getCreatedAt(),
                instance.getUpdatedAt()
        );
    }
}
