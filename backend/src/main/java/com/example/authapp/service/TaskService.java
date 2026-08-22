package com.example.authapp.service;

import com.example.authapp.dto.TaskSummaryDTO;
import com.example.authapp.entity.*;
import com.example.authapp.exception.WorkflowException;
import com.example.authapp.repository.RequestRepository;
import com.example.authapp.repository.WorkflowTaskRepository;
import com.example.authapp.workflow.engine.WorkflowEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The "my approvals inbox" side of things: list pending tasks for the
 * current user, and let them act on one. Ownership of the *decision* is
 * checked here (is this really your task?) before WorkflowEngine is asked
 * to run the actual state transition.
 */
@Service
public class TaskService {

    @Autowired private WorkflowTaskRepository workflowTaskRepository;
    @Autowired private RequestRepository requestRepository;
    @Autowired private WorkflowEngine workflowEngine;

    public List<TaskSummaryDTO> listMyTasks(User user) {
        return workflowTaskRepository.findByAssignedToAndStatusOrderByCreatedAtDesc(user, TaskStatus.PENDING).stream()
                .map(task -> {
                    WorkflowInstance instance = task.getWorkflowInstance();
                    Request request = requestRepository.findByWorkflowInstance(instance).orElse(null);
                    return new TaskSummaryDTO(
                            task.getId(),
                            request != null ? request.getId() : null,
                            instance.getId(),
                            request != null ? request.getTitle() : "(request not found)",
                            request != null ? request.getRequestType() : null,
                            task.getStep().getName(),
                            instance.getCreatedBy().getUsername(),
                            task.getCreatedAt()
                    );
                })
                .toList();
    }

    @Transactional
    public void approve(Long taskId, User actor, String comment) {
        decide(taskId, actor, ApprovalDecision.APPROVED, comment);
    }

    @Transactional
    public void reject(Long taskId, User actor, String comment) {
        decide(taskId, actor, ApprovalDecision.REJECTED, comment);
    }

    @Transactional
    public void requestInformation(Long taskId, User actor, String comment) {
        decide(taskId, actor, ApprovalDecision.INFO_REQUESTED, comment);
    }

    private void decide(Long taskId, User actor, ApprovalDecision decision, String comment) {
        WorkflowTask task = workflowTaskRepository.findById(taskId)
                .orElseThrow(() -> new WorkflowException("Task " + taskId + " not found."));

        boolean isAssignee = task.getAssignedTo().getId().equals(actor.getId());
        boolean isAdmin = actor.getRoles().contains(ERole.ROLE_ADMIN);
        if (!isAssignee && !isAdmin) {
            throw new AccessDeniedException("This task is not assigned to you.");
        }

        workflowEngine.decide(task, decision, actor, comment);
    }
}
