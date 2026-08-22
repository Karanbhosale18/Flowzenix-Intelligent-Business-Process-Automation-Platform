package com.example.authapp.repository;

import com.example.authapp.entity.Approval;
import com.example.authapp.entity.WorkflowInstance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalRepository extends JpaRepository<Approval, Long> {
    List<Approval> findByWorkflowInstanceOrderByCreatedAtAsc(WorkflowInstance workflowInstance);
}
