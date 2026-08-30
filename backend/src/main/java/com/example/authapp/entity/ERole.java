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
    ROLE_ADMIN,
    ROLE_ENGINEERING_AND_DEVELOPER,
    ROLE_MARKETING,
    ROLE_SALES_AND_BUSINESS_DEVELOPMENT,
    ROLE_TECHNICAL_SUPPORT_HELP_DESK,
    ROLE_RND_MANAGER,
    ROLE_RND_ENGINEER,
    ROLE_RND_ANALYST,
    ROLE_FINANCE_MANAGER,
    ROLE_ACCOUNTANT,
    ROLE_FINANCE_ANALYST,
    ROLE_MARKETING_MANAGER,
    ROLE_MARKETING_SPECIALIST,
    ROLE_MARKETING_ANALYST,
    ROLE_SALES_MANAGER,
    ROLE_SALES_EXECUTIVE,
    ROLE_BUSINESS_DEVELOPMENT_MANAGER,
    ROLE_ACCOUNT_MANAGER,
    ROLE_SUPPORT_MANAGER,
    ROLE_SUPPORT_ENGINEER,
    ROLE_HELP_DESK_AGENT,
    ROLE_SYSTEM_ADMIN
}
