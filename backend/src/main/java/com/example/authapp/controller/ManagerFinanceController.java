package com.example.authapp.controller;

import com.example.authapp.dto.FinanceManagerAssignmentDTO;
import com.example.authapp.entity.ERole;
import com.example.authapp.entity.User;
import com.example.authapp.exception.WorkflowException;
import com.example.authapp.payload.request.FinanceManagerAssignmentRequest;
import com.example.authapp.repository.UserRepository;
import com.example.authapp.security.services.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lets a reporting manager choose the Finance Manager for their team's budgets. */
@RestController
@RequestMapping("/api/manager/finance-manager")
public class ManagerFinanceController {

    private final UserRepository userRepository;

    public ManagerFinanceController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public FinanceManagerAssignmentDTO get(@AuthenticationPrincipal UserDetailsImpl principal) {
        User manager = currentManager(principal);
        return toDto(manager);
    }

    @PutMapping
    public ResponseEntity<FinanceManagerAssignmentDTO> update(
            @Valid @RequestBody FinanceManagerAssignmentRequest request,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        User manager = currentManager(principal);
        User financeManager = userRepository.findByfinanceManagerId(request.getFinanceManagerId())
                // Finance Managers created before Finance Manager IDs were introduced
                // used Manager ID. Keep those accounts selectable during the transition.
                .or(() -> userRepository.findByManagerReferenceId(request.getFinanceManagerId()))
                .orElseThrow(() -> new WorkflowException("Finance Manager ID " + request.getFinanceManagerId() + " was not found."));

        if (!financeManager.isEnabled() || !financeManager.getRoles().contains(ERole.ROLE_FINANCE_MANAGER)) {
            throw new WorkflowException("User ID " + request.getFinanceManagerId() + " is not an active Finance Manager.");
        }

        manager.setFinanceManagerId(financeManager.getId());
        userRepository.save(manager);
        return ResponseEntity.ok(toDto(manager));
    }

    private User currentManager(UserDetailsImpl principal) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        boolean isManager = user.getRoles().stream().anyMatch(role -> role.name().endsWith("_MANAGER"));
        if (!isManager) {
            throw new AccessDeniedException("Only managers can choose a Finance Manager for budget requests.");
        }
        return user;
    }

    private FinanceManagerAssignmentDTO toDto(User manager) {
        User financeManager = manager.getFinanceManagerId() == null
                ? null
                : userRepository.findById(manager.getFinanceManagerId()).orElse(null);
        return new FinanceManagerAssignmentDTO(
                financeManager != null ? financeManagerReferenceId(financeManager) : null,
                financeManager != null ? financeManager.getUsername() : null);
    }

    private Long financeManagerReferenceId(User financeManager) {
        return financeManager.getFinanceManagerReferenceId() != null
                ? financeManager.getFinanceManagerReferenceId()
                : financeManager.getManagerReferenceId();
    }
}
