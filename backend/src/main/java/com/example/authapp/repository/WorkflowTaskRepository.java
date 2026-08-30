package com.example.authapp.repository;

import com.example.authapp.entity.TaskStatus;
import com.example.authapp.entity.User;
import com.example.authapp.entity.WorkflowInstance;
import com.example.authapp.entity.WorkflowTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WorkflowTaskRepository extends JpaRepository<WorkflowTask, Long> {
    List<WorkflowTask> findByAssignedToAndStatusOrderByCreatedAtDesc(User assignedTo, TaskStatus status);
    /** Backs "My Tasks" filtering by one or more statuses, e.g. viewing everything you've Approved + Rejected. */
    List<WorkflowTask> findByAssignedToAndStatusInOrderByCreatedAtDesc(User assignedTo, Collection<TaskStatus> statuses);
    Optional<WorkflowTask> findByWorkflowInstanceAndStatus(WorkflowInstance workflowInstance, TaskStatus status);
    List<WorkflowTask> findByWorkflowInstanceOrderByCreatedAtAsc(WorkflowInstance workflowInstance);
}