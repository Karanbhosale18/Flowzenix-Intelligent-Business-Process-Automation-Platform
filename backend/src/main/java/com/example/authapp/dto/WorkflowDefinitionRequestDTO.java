package com.example.authapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class WorkflowDefinitionRequestDTO {
    @NotBlank
    private String name;
    private String description;
    @NotBlank
    private String workflowType;
    /** New definitions are drafts unless explicitly activated after validation. */
    private Boolean active;
    @Valid
    private List<WorkflowStepRequestDTO> steps;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getWorkflowType() { return workflowType; }
    public void setWorkflowType(String workflowType) { this.workflowType = workflowType; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public List<WorkflowStepRequestDTO> getSteps() { return steps; }
    public void setSteps(List<WorkflowStepRequestDTO> steps) { this.steps = steps; }
}
