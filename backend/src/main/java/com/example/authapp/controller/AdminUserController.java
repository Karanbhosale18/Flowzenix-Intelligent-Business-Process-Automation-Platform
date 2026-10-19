package com.example.authapp.controller;

import com.example.authapp.dto.AdminUserDTO;
import com.example.authapp.dto.AdminUserUpdateRequest;
import com.example.authapp.entity.ERole;
import com.example.authapp.entity.User;
import com.example.authapp.exception.WorkflowException;
import com.example.authapp.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ADMIN-only endpoint for role changes, department management,
 * and manager assignments.
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserRepository userRepository;

    public AdminUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<AdminUserDTO>> listUsers() {
        List<AdminUserDTO> users = userRepository.findAll().stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<AdminUserDTO> getUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new WorkflowException("User " + id + " not found."));
        return ResponseEntity.ok(toDto(user));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<AdminUserDTO> updateUser(
            @PathVariable Long id,
            @RequestBody AdminUserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new WorkflowException("User " + id + " not found."));

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            Set<ERole> newRoles = new HashSet<>();
            for (String roleStr : request.getRoles()) {
                String normalized = roleStr.startsWith("ROLE_") ? roleStr : "ROLE_" + roleStr;
                try {
                    newRoles.add(ERole.valueOf(normalized));
                } catch (IllegalArgumentException e) {
                    throw new WorkflowException("Unknown role: " + roleStr);
                }
            }
            user.setRoles(newRoles);
        }

        if (request.getDepartment() != null) {
            user.setDepartment(request.getDepartment().trim());
        }

        if (request.getManagerId() != null) {
            User manager = userRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new WorkflowException("Manager " + request.getManagerId() + " not found."));
            if (manager.getId().equals(user.getId())) {
                throw new WorkflowException("A user cannot be their own manager.");
            }
            user.setManagerId(manager.getId());
        }

        if (request.getAdminId() != null) {
            User admin = userRepository.findById(request.getAdminId())
                    .orElseThrow(() -> new WorkflowException("Admin " + request.getAdminId() + " not found."));
            user.setAdminId(admin.getId());
        }

        if (request.getAdminReferenceId() != null) {
            user.setAdminReferenceId(request.getAdminReferenceId());
        }
        if (request.getManagerReferenceId() != null) {
            user.setManagerReferenceId(request.getManagerReferenceId());
        }
        if (request.getFinanceManagerReferenceId() != null) {
            user.setFinanceManagerReferenceId(request.getFinanceManagerReferenceId());
        }
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        User saved = userRepository.save(user);
        return ResponseEntity.ok(toDto(saved));
    }

    private AdminUserDTO toDto(User user) {
        User manager = user.getManagerId() == null ? null : userRepository.findById(user.getManagerId()).orElse(null);
        String managerName = manager != null ? manager.getUsername() : null;
        return new AdminUserDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRoles(),
                user.getDepartment(),
                user.getManagerId(),
                managerName,
                user.getAdminId(),
                user.getAdminReferenceId(),
                user.getManagerReferenceId(),
                user.getFinanceManagerReferenceId(),
                user.getFinanceManagerId(),
                user.isEnabled()
        );
    }
}
