package com.example.authapp.dto;

import jakarta.validation.constraints.NotBlank;

/** Payload used when creating or editing a workflow step. */
public class WorkflowStepRequestDTO {
    private Integer stepOrder;

    @NotBlank
    private String name;

    @NotBlank
    private String stepType;

    @NotBlank
    private String assignedRole;

    private Boolean required = true;
    private String configuration;

    public Integer getStepOrder() { return stepOrder; }
    public void setStepOrder(Integer stepOrder) { this.stepOrder = stepOrder; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStepType() { return stepType; }
    public void setStepType(String stepType) { this.stepType = stepType; }
    public String getAssignedRole() { return assignedRole; }
    public void setAssignedRole(String assignedRole) { this.assignedRole = assignedRole; }
    public Boolean getRequired() { return required; }
    public void setRequired(Boolean required) { this.required = required; }
    public String getConfiguration() { return configuration; }
    public void setConfiguration(String configuration) { this.configuration = configuration; }
}
