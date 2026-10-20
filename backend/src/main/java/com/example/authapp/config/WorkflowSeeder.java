package com.example.authapp.config;

import com.example.authapp.entity.ERole;
import com.example.authapp.entity.StepType;
import com.example.authapp.entity.WorkflowDefinition;
import com.example.authapp.entity.WorkflowStep;
import com.example.authapp.repository.WorkflowDefinitionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.authapp.entity.User;
import com.example.authapp.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Seeds initial workflow definitions and default bootstrap admin account
 * idempotently on startup.
 */
@Component
public class WorkflowSeeder implements CommandLineRunner {

    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public WorkflowSeeder(WorkflowDefinitionRepository workflowDefinitionRepository,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedBootstrapAdmin();
        seedLeaveRequest();
        seedBudgetRequest();
    }

    private void seedBootstrapAdmin() {
        Optional<User> existingAdmin = userRepository.findByUsername("admin");
        if (existingAdmin.isPresent()) {
            User admin = existingAdmin.get();
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            if (admin.getRoles() == null || admin.getRoles().isEmpty()) {
                admin.setRoles(Set.of(ERole.ROLE_ADMIN));
            } else if (!admin.getRoles().contains(ERole.ROLE_ADMIN)) {
                Set<ERole> roles = new HashSet<>(admin.getRoles());
                roles.add(ERole.ROLE_ADMIN);
                admin.setRoles(roles);
            }
            admin.setEnabled(true);
            userRepository.save(admin);
            return;
        }

        if (userRepository.findFirstByRole(ERole.ROLE_ADMIN).isPresent()) return;

        User admin = new User(
                "admin",
                "admin@flowzenix.com",
                passwordEncoder.encode("Admin@123")
        );
        admin.setRoles(Set.of(ERole.ROLE_ADMIN));
        admin.setDepartment("Admin");
        admin.setAdminReferenceId(1001L);
        admin.setEnabled(true);
        userRepository.save(admin);
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
