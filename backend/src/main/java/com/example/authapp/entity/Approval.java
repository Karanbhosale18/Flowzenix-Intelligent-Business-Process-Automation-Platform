package com.example.authapp.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * A recorded decision on a workflow instance. Kept separate from
 * WorkflowTask (rather than just adding a decision column to the task) so
 * a full decision audit trail survives even if task semantics change later.
 */
@Entity
@Table(name = "approvals")
public class Approval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_instance_id", nullable = false)
    private WorkflowInstance workflowInstance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id", nullable = false)
    private User approver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalDecision decision;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Approval() {
    }

    public Approval(WorkflowInstance workflowInstance, User approver, ApprovalDecision decision, String comment) {
        this.workflowInstance = workflowInstance;
        this.approver = approver;
        this.decision = decision;
        this.comment = comment;
    }

    // ---------- getters / setters ----------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public WorkflowInstance getWorkflowInstance() { return workflowInstance; }
    public void setWorkflowInstance(WorkflowInstance workflowInstance) { this.workflowInstance = workflowInstance; }

    public User getApprover() { return approver; }
    public void setApprover(User approver) { this.approver = approver; }

    public ApprovalDecision getDecision() { return decision; }
    public void setDecision(ApprovalDecision decision) { this.decision = decision; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Instant getCreatedAt() { return createdAt; }
}
