package com.example.authapp.controller;

import com.example.authapp.dto.UserProfileDTO;
import com.example.authapp.entity.ERole;
import com.example.authapp.entity.User;
import com.example.authapp.payload.request.UpdateProfileRequest;
import com.example.authapp.repository.UserRepository;
import com.example.authapp.security.services.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private static final Set<String> ALLOWED_DEPARTMENTS = Set.of(
            "Research & Development", "Finance & Accounting", "Marketing",
            "Sales & Business Development", "Technical Support / Help Desk", "Admin");

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    public ProfileController(UserRepository userRepository, PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    @GetMapping
    public UserProfileDTO get(@AuthenticationPrincipal UserDetailsImpl principal) {
        return toDto(currentUser(principal));
    }

    @PutMapping
    public ResponseEntity<UserProfileDTO> update(@Valid @RequestBody UpdateProfileRequest update,
                                                 @AuthenticationPrincipal UserDetailsImpl principal) {
        User user = currentUser(principal);
        boolean isAdmin = user.getRoles().contains(ERole.ROLE_ADMIN);

        // Security requirement: department management and manager assignments are restricted
        // to authorized administrative operations.
        if (!isAdmin && (update.getDepartment() != null || update.getManagerId() != null)) {
            boolean deptChanged = update.getDepartment() != null && !update.getDepartment().equals(user.getDepartment());
            boolean managerChanged = update.getManagerId() != null && !update.getManagerId().equals(user.getManagerId());
            if (deptChanged || managerChanged) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
            }
        }

        if (isAdmin) {
            if (update.getDepartment() != null) {
                if (!ALLOWED_DEPARTMENTS.contains(update.getDepartment())) {
                    return ResponseEntity.badRequest().build();
                }
                user.setDepartment(update.getDepartment());
            }
            if (update.getManagerId() != null) {
                User manager = userRepository.findByManagerReferenceId(update.getManagerId()).orElse(null);
                boolean validManager = manager != null
                        && !manager.getId().equals(user.getId())
                        && manager.getRoles().stream().anyMatch(role -> role.name().endsWith("_MANAGER"))
                        && (user.getDepartment() == null || user.getDepartment().equals(manager.getDepartment()));
                if (!validManager) {
                    return ResponseEntity.badRequest().build();
                }
                user.setManagerId(manager.getId());
            }
        }

        if (update.getPassword() != null && !update.getPassword().isBlank()) {
            user.setPassword(encoder.encode(update.getPassword()));
        }
        return ResponseEntity.ok(toDto(userRepository.save(user)));
    }

    private User currentUser(UserDetailsImpl principal) {
        return userRepository.findById(principal.getId()).orElseThrow();
    }

    private UserProfileDTO toDto(User user) {
        User manager = user.getManagerId() == null ? null : userRepository.findById(user.getManagerId()).orElse(null);
        String managerName = manager != null ? manager.getUsername() : null;
        Long assignedManagerReferenceId = manager != null ? manager.getManagerReferenceId() : null;
        return new UserProfileDTO(user.getId(), user.getUsername(), user.getEmail(), user.getDepartment(), user.getManagerId(), managerName, user.getAdminReferenceId(), user.getManagerReferenceId(), assignedManagerReferenceId);
    }
}
