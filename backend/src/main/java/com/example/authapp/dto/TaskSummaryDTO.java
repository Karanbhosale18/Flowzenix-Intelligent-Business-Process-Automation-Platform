package com.example.authapp.dto;

import java.time.Instant;

/** Row shape for "My Tasks" / Pending Approvals inbox. */
public class TaskSummaryDTO {
    private Long taskId;
    private Long requestId;
    private Long workflowInstanceId;
    private String requestTitle;
    private String requestType;
    private String stepName;
    private String requestedBy;
    private Instant createdAt;

    public TaskSummaryDTO() {
    }

    public TaskSummaryDTO(Long taskId, Long requestId, Long workflowInstanceId, String requestTitle,
                           String requestType, String stepName, String requestedBy, Instant createdAt) {
        this.taskId = taskId;
        this.requestId = requestId;
        this.workflowInstanceId = workflowInstanceId;
        this.requestTitle = requestTitle;
        this.requestType = requestType;
        this.stepName = stepName;
        this.requestedBy = requestedBy;
        this.createdAt = createdAt;
    }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }

    public Long getWorkflowInstanceId() { return workflowInstanceId; }
    public void setWorkflowInstanceId(Long workflowInstanceId) { this.workflowInstanceId = workflowInstanceId; }

    public String getRequestTitle() { return requestTitle; }
    public void setRequestTitle(String requestTitle) { this.requestTitle = requestTitle; }

    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }

    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }

    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
