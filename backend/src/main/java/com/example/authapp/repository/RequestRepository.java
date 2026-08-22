package com.example.authapp.repository;

import com.example.authapp.entity.Request;
import com.example.authapp.entity.WorkflowInstance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, Long> {
    Optional<Request> findByWorkflowInstance(WorkflowInstance workflowInstance);
}
