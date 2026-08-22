package com.example.authapp.repository;

import com.example.authapp.entity.WorkflowHistory;
import com.example.authapp.entity.WorkflowInstance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowHistoryRepository extends JpaRepository<WorkflowHistory, Long> {
    List<WorkflowHistory> findByWorkflowInstanceOrderByCreatedAtAsc(WorkflowInstance workflowInstance);
}
