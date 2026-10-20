package com.example.authapp.workflow.engine;

import com.example.authapp.entity.*;
import com.example.authapp.exception.WorkflowException;
import com.example.authapp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * The reusable workflow engine. This is the ONE piece of code that
 * understands how to run *any* workflow definition — it never branches on
 * request type. Adding "IT_SUPPORT" or "EXPENSE_REIMBURSEMENT" means
 * inserting a new WorkflowDefinition + WorkflowSteps (via the admin builder),
 * not touching this class.
 *
 * Responsibilities:
 *  - start(): create a WorkflowInstance for a definition, resolve and
 *    create the first task, write history.
 *  - decide(): record an approve/reject/request-information decision on a
 *    task, advance to the next step (or finish the workflow), write history.
 *  - resolveAssignee(): map a WorkflowStep's assignedRole to an actual
 *    person — the employee's manager for ROLE_MANAGER, their manager's
 *    selected Finance Manager for budget approvals, otherwise the first
 *    enabled user holding that role.
 */
@Service
public class WorkflowEngine {

    @Autowired
    private WorkflowInstanceRepository workflowInstanceRepository;

    @Autowired
    private WorkflowTaskRepository workflowTaskRepository;

    @Autowired
    private ApprovalRepository approvalRepository;

    @Autowired
    private WorkflowHistoryRepository workflowHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired(required = false)
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    /**
     * Starts a new workflow instance from the given definition and creates
     * the task for its first step. Definitions with zero steps are treated
     * as a configuration error, not silently auto-completed.
     */
    @Transactional
    public WorkflowInstance start(WorkflowDefinition definition, User submittedBy) {
        List<WorkflowStep> steps = definition.getOrderedSteps();
        if (steps.isEmpty()) {
            throw new WorkflowException(
                    "Workflow definition '" + definition.getName() + "' has no steps configured.");
        }

        WorkflowInstance instance = new WorkflowInstance(definition, submittedBy);
        instance.setStatus(WorkflowStatus.SUBMITTED);
        instance.setCurrentStep(0);
        instance = workflowInstanceRepository.save(instance);

        recordHistory(instance, "Request submitted", submittedBy, null, WorkflowStatus.SUBMITTED, null);

        WorkflowTask firstTask = advanceToStep(instance, steps.get(0), submittedBy);
        publishEvent(com.example.authapp.workflow.event.WorkflowEvent.EventType.SUBMITTED,
                instance, firstTask, submittedBy, null);

        return instance;
    }

    /**
     * Records a human decision on a task and advances (or ends) the
     * workflow accordingly. `actor` must be the task's assignedTo — the
     * caller (RequestService/controller) is responsible for that check so
     * this method can stay focused on workflow transitions.
     */
    @Transactional
    public void decide(WorkflowTask task, ApprovalDecision decision, User actor, String comment) {
        if (task.getStatus() != TaskStatus.PENDING) {
            throw new WorkflowException("This task has already been resolved.");
        }

        WorkflowInstance instance = task.getWorkflowInstance();
        WorkflowStatus previousStatus = instance.getStatus();

        approvalRepository.save(new Approval(instance, actor, decision, comment));

        switch (decision) {
            case APPROVED -> {
                task.setStatus(TaskStatus.APPROVED);
                task.setComment(comment);
                task.setCompletedAt(Instant.now());
                workflowTaskRepository.save(task);

                recordHistory(instance, task.getStep().getName() + " approved", actor,
                        previousStatus, previousStatus, comment);

                List<WorkflowStep> steps = instance.getWorkflowDefinition().getOrderedSteps();
                int nextOrder = task.getStep().getStepOrder() + 1;
                Optional<WorkflowStep> nextStep = steps.stream()
                        .filter(s -> s.getStepOrder() == nextOrder)
                        .findFirst();

                if (nextStep.isPresent()) {
                    WorkflowTask nextTask = advanceToStep(instance, nextStep.get(), actor);
                    publishEvent(com.example.authapp.workflow.event.WorkflowEvent.EventType.APPROVED,
                            instance, nextTask, actor, comment);
                } else {
                    instance.setStatus(WorkflowStatus.APPROVED);
                    instance.setCompletedAt(Instant.now());
                    instance.touch();
                    workflowInstanceRepository.save(instance);
                    recordHistory(instance, "Workflow completed", actor, previousStatus, WorkflowStatus.APPROVED, null);
                    publishEvent(com.example.authapp.workflow.event.WorkflowEvent.EventType.APPROVED,
                            instance, task, actor, comment);
                }
            }
            case REJECTED -> {
                task.setStatus(TaskStatus.REJECTED);
                task.setComment(comment);
                task.setCompletedAt(Instant.now());
                workflowTaskRepository.save(task);

                instance.setStatus(WorkflowStatus.REJECTED);
                instance.setCompletedAt(Instant.now());
                instance.touch();
                workflowInstanceRepository.save(instance);

                recordHistory(instance, task.getStep().getName() + " rejected", actor,
                        previousStatus, WorkflowStatus.REJECTED, comment);
                publishEvent(com.example.authapp.workflow.event.WorkflowEvent.EventType.REJECTED,
                        instance, task, actor, comment);
            }
            case INFO_REQUESTED -> {
                task.setStatus(TaskStatus.INFO_REQUESTED);
                task.setComment(comment);
                workflowTaskRepository.save(task);

                instance.setStatus(WorkflowStatus.PENDING_INFORMATION);
                instance.touch();
                workflowInstanceRepository.save(instance);

                recordHistory(instance, "Additional information requested", actor,
                        previousStatus, WorkflowStatus.PENDING_INFORMATION, comment);
                publishEvent(com.example.authapp.workflow.event.WorkflowEvent.EventType.INFO_REQUESTED,
                        instance, task, actor, comment);
            }
        }
    }

    /**
     * Resumes a workflow that was parked in PENDING_INFORMATION when an approver
     * requested additional info. The requester provides their response,
     * restoring the pending task and setting instance status back to pending approval.
     */
    @Transactional
    public void provideInformation(WorkflowInstance instance, User actor, String comment) {
        if (instance.getStatus() != WorkflowStatus.PENDING_INFORMATION) {
            throw new WorkflowException(
                    "Cannot provide information for a request that is currently " + instance.getStatus() + ".");
        }

        WorkflowTask task = workflowTaskRepository.findByWorkflowInstanceAndStatus(instance, TaskStatus.INFO_REQUESTED)
                .orElseThrow(() -> new WorkflowException("No task found awaiting information for this request."));

        WorkflowStatus previousStatus = instance.getStatus();
        WorkflowStatus newStatus = statusForRole(task.getStep().getAssignedRole());

        task.setStatus(TaskStatus.PENDING);
        workflowTaskRepository.save(task);

        instance.setStatus(newStatus);
        instance.touch();
        workflowInstanceRepository.save(instance);

        recordHistory(instance, "Additional information provided", actor,
                previousStatus, newStatus, comment);

        publishEvent(com.example.authapp.workflow.event.WorkflowEvent.EventType.INFO_PROVIDED,
                instance, task, actor, comment);
    }

    /**
     * Cancels a still-running workflow instance on behalf of the requester.
     * Unlike decide(), cancellation is initiated by the request's owner
     * (that ownership check lives in RequestService), not by the current
     * task's assignee. A workflow that has already reached a terminal state
     * cannot be cancelled — attempting to do so is a workflow error, not a
     * silent no-op, so the caller gets a clear 422 rather than a misleading
     * "success". Keeping this here (rather than mutating the instance from a
     * service) preserves the invariant that WorkflowInstance.status only
     * ever changes inside the engine, so workflow_history stays a complete
     * record of every transition.
     */
    @Transactional
    public void cancel(WorkflowInstance instance, User actor) {
        WorkflowStatus previousStatus = instance.getStatus();
        if (isTerminal(previousStatus)) {
            throw new WorkflowException(
                    "Cannot cancel a request that is already " + previousStatus + ".");
        }

        // Close out the open task (if any) so it drops off its assignee's
        // pending-approvals inbox. There is at most one PENDING task per
        // instance in this single-step-at-a-time engine.
        java.util.concurrent.atomic.AtomicReference<WorkflowTask> cancelledTask = new java.util.concurrent.atomic.AtomicReference<>();
        workflowTaskRepository.findByWorkflowInstanceAndStatus(instance, TaskStatus.PENDING)
                .ifPresent(task -> {
                    task.setStatus(TaskStatus.CANCELLED);
                    task.setCompletedAt(Instant.now());
                    cancelledTask.set(workflowTaskRepository.save(task));
                });

        instance.setStatus(WorkflowStatus.CANCELLED);
        instance.setCompletedAt(Instant.now());
        instance.touch();
        workflowInstanceRepository.save(instance);

        recordHistory(instance, "Request cancelled", actor, previousStatus, WorkflowStatus.CANCELLED, null);
        publishEvent(com.example.authapp.workflow.event.WorkflowEvent.EventType.CANCELLED,
                instance, cancelledTask.get(), actor, null);
    }

    /** A terminal instance can no longer be advanced, decided on, or cancelled. */
    private boolean isTerminal(WorkflowStatus status) {
        return status == WorkflowStatus.APPROVED
                || status == WorkflowStatus.REJECTED
                || status == WorkflowStatus.CANCELLED
                || status == WorkflowStatus.COMPLETED;
    }

    /** Creates the WorkflowTask for `step`, resolves its assignee, and updates instance status/position. */
    private WorkflowTask advanceToStep(WorkflowInstance instance, WorkflowStep step, User actor) {
        User assignee = resolveAssignee(instance, step);

        WorkflowTask task = new WorkflowTask(instance, step, assignee);
        task = workflowTaskRepository.save(task);

        WorkflowStatus previousStatus = instance.getStatus();
        WorkflowStatus newStatus = statusForRole(step.getAssignedRole());

        instance.setCurrentStep(step.getStepOrder());
        instance.setStatus(newStatus);
        instance.touch();
        workflowInstanceRepository.save(instance);

        recordHistory(instance, "Assigned to " + step.getName() + " (" + assignee.getUsername() + ")",
                actor, previousStatus, newStatus, null);

        return task;
    }

    /**
     * ROLE_MANAGER resolves to the requester's own reporting manager
     * (User.managerId). The Finance step resolves to that reporting manager's
     * saved Finance Manager; this makes delegation explicit and stable.
     * Generic engine rule: applies to any workflow definition requiring Finance.
     * Other roles resolve to the first enabled user holding the requested role.
     */
    private User resolveAssignee(WorkflowInstance instance, WorkflowStep step) {
        ERole role = step.getAssignedRole();
        if (role == null) {
            throw new WorkflowException("Step '" + step.getName() + "' has no assigned role configured.");
        }

        User requester = instance.getCreatedBy();

        if (role == ERole.ROLE_FINANCE || role == ERole.ROLE_FINANCE_MANAGER) {
            User manager = reportingManagerFor(instance);
            Long financeManagerId = manager.getFinanceManagerId();
            if (financeManagerId != null) {
                Optional<User> fm = userRepository.findById(financeManagerId);
                if (fm.isPresent() && fm.get().isEnabled() && fm.get().getRoles().contains(ERole.ROLE_FINANCE_MANAGER)) {
                    return fm.get();
                }
            }
            return userRepository.findFirstByRole(ERole.ROLE_FINANCE_MANAGER)
                    .or(() -> userRepository.findFirstByRole(ERole.ROLE_FINANCE))
                    .or(() -> userRepository.findFirstByRole(ERole.ROLE_ADMIN))
                    .orElse(manager);
        }

        // Every approval step for a manager-submitted request is sent to the
        // manager's configured Admin.
        if (requester.getRoles().stream().anyMatch(r -> r.name().endsWith("_MANAGER"))) {
            Long adminId = requester.getAdminId();
            if (adminId != null) {
                Optional<User> admin = userRepository.findById(adminId);
                if (admin.isPresent() && admin.get().getRoles().contains(ERole.ROLE_ADMIN)) {
                    return admin.get();
                }
            }
            return userRepository.findFirstByRole(ERole.ROLE_ADMIN).orElse(requester);
        }

        if (role == ERole.ROLE_MANAGER) {
            Long managerId = requester.getManagerId();
            if (managerId != null) {
                Optional<User> manager = userRepository.findById(managerId);
                if (manager.isPresent() && manager.get().isEnabled()) {
                    return manager.get();
                }
            }
            return userRepository.findFirstByRole(ERole.ROLE_MANAGER)
                    .or(() -> userRepository.findFirstByRole(ERole.ROLE_ADMIN))
                    .orElse(requester);
        }

        return userRepository.findFirstByRole(role)
                .or(() -> userRepository.findFirstByRole(ERole.ROLE_ADMIN))
                .orElse(requester);
    }

    private User reportingManagerFor(WorkflowInstance instance) {
        User requester = instance.getCreatedBy();
        if (requester.getRoles().stream().anyMatch(role -> role.name().endsWith("_MANAGER") || role == ERole.ROLE_ADMIN)) {
            return requester;
        }
        Long managerId = requester.getManagerId();
        if (managerId != null) {
            Optional<User> manager = userRepository.findById(managerId);
            if (manager.isPresent() && manager.get().isEnabled()) {
                return manager.get();
            }
        }
        return userRepository.findFirstByRole(ERole.ROLE_MANAGER)
                .or(() -> userRepository.findFirstByRole(ERole.ROLE_ADMIN))
                .orElse(requester);
    }

    private WorkflowStatus statusForRole(ERole role) {
        if (role == null) return WorkflowStatus.SUBMITTED;
        return switch (role) {
            case ROLE_MANAGER -> WorkflowStatus.PENDING_MANAGER_APPROVAL;
            case ROLE_FINANCE -> WorkflowStatus.PENDING_FINANCE_APPROVAL;
            case ROLE_HR -> WorkflowStatus.PENDING_HR_APPROVAL;
            case ROLE_IT_ADMIN -> WorkflowStatus.PENDING_IT_ADMIN_APPROVAL;
            default -> WorkflowStatus.SUBMITTED;
        };
    }

    private void recordHistory(WorkflowInstance instance, String action, User performedBy,
                                WorkflowStatus oldStatus, WorkflowStatus newStatus, String comment) {
        workflowHistoryRepository.save(
                new WorkflowHistory(instance, action, performedBy, oldStatus, newStatus, comment));
    }

    private void publishEvent(com.example.authapp.workflow.event.WorkflowEvent.EventType type,
                              WorkflowInstance instance, WorkflowTask task, User actor, String comment) {
        if (eventPublisher != null) {
            eventPublisher.publishEvent(
                    new com.example.authapp.workflow.event.WorkflowEvent(type, instance, task, actor, comment));
        }
    }
}
