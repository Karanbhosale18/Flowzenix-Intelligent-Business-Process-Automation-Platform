package com.example.authapp.repository;

import com.example.authapp.entity.WorkflowDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, Long> {
    Optional<WorkflowDefinition> findByWorkflowTypeAndActiveTrue(String workflowType);
    boolean existsByWorkflowType(String workflowType);
}
