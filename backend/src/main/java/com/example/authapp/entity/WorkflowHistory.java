package com.example.authapp.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Append-only audit trail: one row per meaningful event on a
 * WorkflowInstance ("submitted", "AI classified", "assigned to manager",
 * "manager approved", ...). This is what powers the timeline on the
 * Request Details page.
 */
@Entity
@Table(name = "workflow_history")
public class WorkflowHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_instance_id", nullable = false)
    private WorkflowInstance workflowInstance;

    @Column(nullable = false)
    private String action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    private User performedBy;

    @Enumerated(EnumType.STRING)
    private WorkflowStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkflowStatus newStatus;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public WorkflowHistory() {
    }

    public WorkflowHistory(WorkflowInstance workflowInstance, String action, User performedBy,
                            WorkflowStatus oldStatus, WorkflowStatus newStatus, String comment) {
        this.workflowInstance = workflowInstance;
        this.action = action;
        this.performedBy = performedBy;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.comment = comment;
    }

    // ---------- getters / setters ----------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public WorkflowInstance getWorkflowInstance() { return workflowInstance; }
    public void setWorkflowInstance(WorkflowInstance workflowInstance) { this.workflowInstance = workflowInstance; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public User getPerformedBy() { return performedBy; }
    public void setPerformedBy(User performedBy) { this.performedBy = performedBy; }

    public WorkflowStatus getOldStatus() { return oldStatus; }
    public void setOldStatus(WorkflowStatus oldStatus) { this.oldStatus = oldStatus; }

    public WorkflowStatus getNewStatus() { return newStatus; }
    public void setNewStatus(WorkflowStatus newStatus) { this.newStatus = newStatus; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Instant getCreatedAt() { return createdAt; }
}
