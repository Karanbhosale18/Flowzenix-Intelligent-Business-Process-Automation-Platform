package com.example.authapp.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A reusable "template" for a workflow: LEAVE_REQUEST, BUDGET_REQUEST,
 * IT_SUPPORT, and so on. Adding a new request type means inserting a new
 * WorkflowDefinition + its WorkflowSteps — never touching WorkflowEngine's
 * code. This is what makes the platform an *automation platform* rather
 * than one controller branch per request type.
 */
@Entity
@Table(name = "workflow_definitions")
public class WorkflowDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    private String description;

    /**
     * Matches Request.requestType, e.g. "LEAVE_REQUEST". Unique so lookup
     * by type is unambiguous ("find the active definition for this type").
     */
    @NotBlank
    @Column(nullable = false, unique = true)
    private String workflowType;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "workflowDefinition", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkflowStep> steps = new ArrayList<>();

    public WorkflowDefinition() {
    }

    public WorkflowDefinition(String name, String description, String workflowType) {
        this.name = name;
        this.description = description;
        this.workflowType = workflowType;
    }

    /** Steps in execution order — the engine always walks this list in order. */
    public List<WorkflowStep> getOrderedSteps() {
        return steps.stream()
                .sorted(Comparator.comparingInt(WorkflowStep::getStepOrder))
                .toList();
    }

    // ---------- getters / setters ----------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getWorkflowType() { return workflowType; }
    public void setWorkflowType(String workflowType) { this.workflowType = workflowType; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }

    public List<WorkflowStep> getSteps() { return steps; }
    public void setSteps(List<WorkflowStep> steps) { this.steps = steps; }
}
