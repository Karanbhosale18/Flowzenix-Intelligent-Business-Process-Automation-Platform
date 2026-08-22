package com.example.authapp.dto;

import com.example.authapp.entity.Priority;
import com.example.authapp.entity.WorkflowStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Full shape for the Request Details page: request + timeline + steps + "can I act on this". */
public class RequestDetailDTO {
    private Long requestId;
    private Long workflowInstanceId;
    private String requestType;
    private String title;
    private String description;
    private Priority priority;
    private Map<String, Object> metadata;
    private WorkflowStatus status;
    private Instant createdAt;
    private Instant completedAt;

    private List<StepSummaryDTO> steps;
    private List<HistoryEntryDTO> history;

    /** Non-null and equal to a pending WorkflowTask id when the current user can act on it right now. */
    private Long myPendingTaskId;

    public record StepSummaryDTO(int stepOrder, String name, String assignedRole, boolean current, boolean completed) {}
    public record HistoryEntryDTO(String action, String performedBy, String oldStatus, String newStatus, String comment, Instant createdAt) {}

    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }

    public Long getWorkflowInstanceId() { return workflowInstanceId; }
    public void setWorkflowInstanceId(Long workflowInstanceId) { this.workflowInstanceId = workflowInstanceId; }

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

    public WorkflowStatus getStatus() { return status; }
    public void setStatus(WorkflowStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public List<StepSummaryDTO> getSteps() { return steps; }
    public void setSteps(List<StepSummaryDTO> steps) { this.steps = steps; }

    public List<HistoryEntryDTO> getHistory() { return history; }
    public void setHistory(List<HistoryEntryDTO> history) { this.history = history; }

    public Long getMyPendingTaskId() { return myPendingTaskId; }
    public void setMyPendingTaskId(Long myPendingTaskId) { this.myPendingTaskId = myPendingTaskId; }
}
