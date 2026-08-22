package com.example.authapp.entity;

import jakarta.persistence.*;

/**
 * One stage in a WorkflowDefinition, e.g. "Manager Approval". Assigned to a
 * ROLE, not a person — WorkflowEngine resolves the actual assignee (the
 * employee's manager, or the first Finance user, etc.) when it creates the
 * WorkflowTask for this step.
 */
@Entity
@Table(name = "workflow_steps")
public class WorkflowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_definition_id", nullable = false)
    private WorkflowDefinition workflowDefinition;

    /** 1-based order within the definition. The engine advances currentStepOrder + 1. */
    @Column(nullable = false)
    private int stepOrder;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StepType stepType;

    /** Role this step routes to. Null only makes sense for non-APPROVAL step types. */
    @Enumerated(EnumType.STRING)
    private ERole assignedRole;

    @Column(nullable = false)
    private boolean required = true;

    /** Free-form config for future step behaviour (thresholds, conditions, etc). */
    @Column(columnDefinition = "TEXT")
    private String configuration;

    public WorkflowStep() {
    }

    public WorkflowStep(WorkflowDefinition workflowDefinition, int stepOrder, String name,
                         StepType stepType, ERole assignedRole, boolean required) {
        this.workflowDefinition = workflowDefinition;
        this.stepOrder = stepOrder;
        this.name = name;
        this.stepType = stepType;
        this.assignedRole = assignedRole;
        this.required = required;
    }

    // ---------- getters / setters ----------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public WorkflowDefinition getWorkflowDefinition() { return workflowDefinition; }
    public void setWorkflowDefinition(WorkflowDefinition workflowDefinition) { this.workflowDefinition = workflowDefinition; }

    public int getStepOrder() { return stepOrder; }
    public void setStepOrder(int stepOrder) { this.stepOrder = stepOrder; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public StepType getStepType() { return stepType; }
    public void setStepType(StepType stepType) { this.stepType = stepType; }

    public ERole getAssignedRole() { return assignedRole; }
    public void setAssignedRole(ERole assignedRole) { this.assignedRole = assignedRole; }

    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }

    public String getConfiguration() { return configuration; }
    public void setConfiguration(String configuration) { this.configuration = configuration; }
}
