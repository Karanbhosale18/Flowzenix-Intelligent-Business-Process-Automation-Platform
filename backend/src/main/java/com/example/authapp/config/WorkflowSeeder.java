package com.example.authapp.config;

import com.example.authapp.entity.ERole;
import com.example.authapp.entity.StepType;
import com.example.authapp.entity.WorkflowDefinition;
import com.example.authapp.entity.WorkflowStep;
import com.example.authapp.repository.WorkflowDefinitionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds the two workflow definitions the demo scenario needs
 * (LEAVE_REQUEST, BUDGET_REQUEST) on startup, idempotently. This is
 * intentionally the ONLY place that knows what these two workflows look
 * like — WorkflowEngine has no idea LEAVE_REQUEST or BUDGET_REQUEST exist.
 * An admin-facing workflow builder (Phase 6) would replace this file with
 * a UI that writes the same rows.
 */
@Component
public class WorkflowSeeder implements CommandLineRunner {

    private final WorkflowDefinitionRepository workflowDefinitionRepository;

    public WorkflowSeeder(WorkflowDefinitionRepository workflowDefinitionRepository) {
        this.workflowDefinitionRepository = workflowDefinitionRepository;
    }

    @Override
    public void run(String... args) {
        seedLeaveRequest();
        seedBudgetRequest();
    }

    private void seedLeaveRequest() {
        if (workflowDefinitionRepository.existsByWorkflowType("LEAVE_REQUEST")) return;

        WorkflowDefinition def = new WorkflowDefinition(
                "Employee Leave Request",
                "Employee submits leave dates; routes to their reporting manager for approval.",
                "LEAVE_REQUEST"
        );
        def.getSteps().add(new WorkflowStep(def, 1, "Manager Approval", StepType.APPROVAL, ERole.ROLE_MANAGER, true));
        workflowDefinitionRepository.save(def);
    }

    private void seedBudgetRequest() {
        if (workflowDefinitionRepository.existsByWorkflowType("BUDGET_REQUEST")) return;

        WorkflowDefinition def = new WorkflowDefinition(
                "Budget Request",
                "Employee requests a budget spend; routes through Manager then Finance for approval.",
                "BUDGET_REQUEST"
        );
        def.getSteps().add(new WorkflowStep(def, 1, "Manager Review", StepType.APPROVAL, ERole.ROLE_MANAGER, true));
        def.getSteps().add(new WorkflowStep(def, 2, "Finance Approval", StepType.APPROVAL, ERole.ROLE_FINANCE, true));
        workflowDefinitionRepository.save(def);
    }
}
