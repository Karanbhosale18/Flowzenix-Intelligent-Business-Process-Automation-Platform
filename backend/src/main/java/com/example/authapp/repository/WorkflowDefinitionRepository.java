package com.example.authapp.repository;

import com.example.authapp.entity.WorkflowDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, Long> {
    Optional<WorkflowDefinition> findByWorkflowTypeAndActiveTrue(String workflowType);
    Optional<WorkflowDefinition> findTopByWorkflowTypeOrderByVersionDesc(String workflowType);
    java.util.List<WorkflowDefinition> findByWorkflowTypeOrderByVersionDesc(String workflowType);
    boolean existsByWorkflowType(String workflowType);
    boolean existsByWorkflowTypeAndVersion(String workflowType, Integer version);
}
