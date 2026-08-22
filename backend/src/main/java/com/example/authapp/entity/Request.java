package com.example.authapp.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * The employee-facing request itself: "I need leave from Sep 10-12" or "I
 * need ₹50,000 for a conference". One Request drives exactly one
 * WorkflowInstance. Type-specific fields (dates, amount, purpose...) live
 * in `metadata` as JSONB rather than as dedicated columns, so a brand new
 * request type never requires a schema migration — only a new
 * WorkflowDefinition.
 */
@Entity
@Table(name = "requests")
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_instance_id", nullable = false, unique = true)
    private WorkflowInstance workflowInstance;

    /** Matches WorkflowDefinition.workflowType, e.g. "LEAVE_REQUEST". */
    @NotBlank
    @Column(nullable = false)
    private String requestType;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority = Priority.MEDIUM;

    /**
     * Structured, type-specific fields — startDate/endDate/reason for a
     * leave request, amount/purpose for a budget request, etc. Stored as
     * JSONB; Hibernate 6 maps Map<String,Object> to it natively.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Request() {
    }

    // ---------- getters / setters ----------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public WorkflowInstance getWorkflowInstance() { return workflowInstance; }
    public void setWorkflowInstance(WorkflowInstance workflowInstance) { this.workflowInstance = workflowInstance; }

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

    public Instant getCreatedAt() { return createdAt; }
}
