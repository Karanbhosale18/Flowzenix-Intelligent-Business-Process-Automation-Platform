package com.example.authapp.dto;

import com.example.authapp.entity.WorkflowStep;

public class WorkflowStepResponseDTO {
    private Long id;
    private int stepOrder;
    private String name;
    private String stepType;
    private String assignedRole;
    private boolean required;
    private String configuration;

    public static WorkflowStepResponseDTO from(WorkflowStep step) {
        WorkflowStepResponseDTO dto = new WorkflowStepResponseDTO();
        dto.id = step.getId();
        dto.stepOrder = step.getStepOrder();
        dto.name = step.getName();
        dto.stepType = step.getStepType() == null ? null : step.getStepType().name();
        dto.assignedRole = step.getAssignedRole() == null ? null : step.getAssignedRole().name();
        dto.required = step.isRequired();
        dto.configuration = step.getConfiguration();
        return dto;
    }

    public Long getId() { return id; }
    public int getStepOrder() { return stepOrder; }
    public String getName() { return name; }
    public String getStepType() { return stepType; }
    public String getAssignedRole() { return assignedRole; }
    public boolean isRequired() { return required; }
    public String getConfiguration() { return configuration; }
}
