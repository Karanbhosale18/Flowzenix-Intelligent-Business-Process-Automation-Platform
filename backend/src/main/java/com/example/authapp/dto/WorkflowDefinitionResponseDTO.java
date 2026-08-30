package com.example.authapp.dto;

import com.example.authapp.entity.WorkflowDefinition;
import java.time.Instant;
import java.util.List;

public class WorkflowDefinitionResponseDTO {
    private Long id;
    private String name;
    private String description;
    private String workflowType;
    private boolean active;
    private Instant createdAt;
    private List<WorkflowStepResponseDTO> steps;

    public static WorkflowDefinitionResponseDTO from(WorkflowDefinition definition) {
        WorkflowDefinitionResponseDTO dto = new WorkflowDefinitionResponseDTO();
        dto.id = definition.getId();
        dto.name = definition.getName();
        dto.description = definition.getDescription();
        dto.workflowType = definition.getWorkflowType();
        dto.active = definition.isActive();
        dto.createdAt = definition.getCreatedAt();
        dto.steps = definition.getOrderedSteps().stream().map(WorkflowStepResponseDTO::from).toList();
        return dto;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getWorkflowType() { return workflowType; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public List<WorkflowStepResponseDTO> getSteps() { return steps; }
}
