package com.example.authapp.entity;

/**
 * Status of a WorkflowInstance. Transitions are enforced by WorkflowEngine —
 * nothing else should mutate WorkflowInstance.status directly, so the
 * history table stays a true record of every legal transition.
 */
public enum WorkflowStatus {
    DRAFT,
    SUBMITTED,
    AI_PROCESSING,
    PENDING_MANAGER_APPROVAL,
    PENDING_FINANCE_APPROVAL,
    PENDING_HR_APPROVAL,
    PENDING_IT_ADMIN_APPROVAL,
    PENDING_INFORMATION,
    APPROVED,
    REJECTED,
    CANCELLED,
    COMPLETED
}
