package com.example.authapp.dto;

import com.example.authapp.entity.Priority;
import jakarta.validation.constraints.NotBlank;

import java.util.HashMap;
import java.util.Map;

/**
 * What the "New Request" form submits. `requestType` must match an active
 * WorkflowDefinition.workflowType (e.g. "LEAVE_REQUEST", "BUDGET_REQUEST")
 * — anything else is rejected before a workflow is started.
 */
public class CreateRequestDTO {

    @NotBlank
    private String requestType;

    @NotBlank
    private String title;

    private String description;

    private Priority priority = Priority.MEDIUM;

    /** Type-specific fields: startDate/endDate/reason, amount/purpose, etc. */
    private Map<String, Object> metadata = new HashMap<>();

    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
