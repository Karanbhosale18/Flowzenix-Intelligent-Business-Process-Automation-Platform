package com.example.authapp.entity;

/**
 * Application roles used for authorization (RBAC) and for resolving who a
 * workflow step routes to. A WorkflowStep is "assigned" to a role, not a
 * specific person — the engine resolves the actual person at runtime
 * (see WorkflowEngine.resolveAssignee).
 */
public enum ERole {
    ROLE_EMPLOYEE,
    ROLE_MANAGER,
    ROLE_FINANCE,
    ROLE_HR,
    ROLE_IT_ADMIN,
    ROLE_ADMIN
}
