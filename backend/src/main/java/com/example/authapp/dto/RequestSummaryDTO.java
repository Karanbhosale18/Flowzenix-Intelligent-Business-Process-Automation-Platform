package com.example.authapp.dto;

import com.example.authapp.entity.Priority;
import com.example.authapp.entity.WorkflowStatus;

import java.time.Instant;

/** Row shape for the "My Requests" table. */
public class RequestSummaryDTO {
    private Long requestId;
    private Long workflowInstanceId;
    private String requestType;
    private String title;
    private Priority priority;
    private WorkflowStatus status;
    private String currentStepName;
    private Instant createdAt;
    private Instant updatedAt;

    public RequestSummaryDTO() {
    }

    public RequestSummaryDTO(Long requestId, Long workflowInstanceId, String requestType, String title,
                              Priority priority, WorkflowStatus status, String currentStepName,
                              Instant createdAt, Instant updatedAt) {
        this.requestId = requestId;
        this.workflowInstanceId = workflowInstanceId;
        this.requestType = requestType;
        this.title = title;
        this.priority = priority;
        this.status = status;
        this.currentStepName = currentStepName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }

    public Long getWorkflowInstanceId() { return workflowInstanceId; }
    public void setWorkflowInstanceId(Long workflowInstanceId) { this.workflowInstanceId = workflowInstanceId; }

    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public WorkflowStatus getStatus() { return status; }
    public void setStatus(WorkflowStatus status) { this.status = status; }

    public String getCurrentStepName() { return currentStepName; }
    public void setCurrentStepName(String currentStepName) { this.currentStepName = currentStepName; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
