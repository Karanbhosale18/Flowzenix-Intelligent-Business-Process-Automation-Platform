package com.example.authapp.controller;

import com.example.authapp.dto.AdminCreateUserRequest;
import com.example.authapp.dto.AdminUserDTO;
import com.example.authapp.dto.AdminUserUpdateRequest;
import com.example.authapp.entity.ERole;
import com.example.authapp.entity.User;
import com.example.authapp.exception.WorkflowException;
import com.example.authapp.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * ADMIN-only endpoint for user management: listing, filtering, search,
 * user creation, role changes, department management, direct reports,
 * and manager assignments with cycle detection.
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Autowired
    public AdminUserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<AdminUserDTO>> listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Boolean enabled) {

        List<AdminUserDTO> users = userRepository.findAll().stream()
                .filter(u -> {
                    if (search == null || search.isBlank()) return true;
                    String term = search.trim().toLowerCase(Locale.ROOT);
                    return (u.getUsername() != null && u.getUsername().toLowerCase(Locale.ROOT).contains(term))
                            || (u.getEmail() != null && u.getEmail().toLowerCase(Locale.ROOT).contains(term));
                })
                .filter(u -> {
                    if (role == null || role.isBlank()) return true;
                    String normRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                    return u.getRoles().stream().anyMatch(r -> r.name().equalsIgnoreCase(normRole));
                })
                .filter(u -> {
                    if (department == null || department.isBlank()) return true;
                    return u.getDepartment() != null && u.getDepartment().equalsIgnoreCase(department.trim());
                })
                .filter(u -> enabled == null || u.isEnabled() == enabled)
                .sorted(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER))
                .map(this::toDto)
                .toList();

        return ResponseEntity.ok(users);
    }


    @GetMapping("/managers")
    @Transactional(readOnly = true)
    public ResponseEntity<List<AdminUserDTO>> listEligibleManagers() {
        List<AdminUserDTO> managers = userRepository.findAll().stream()
                .filter(User::isEnabled)
                .filter(u -> u.getRoles().stream().anyMatch(r -> r.name().endsWith("_MANAGER") || r == ERole.ROLE_ADMIN))
                .sorted(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER))
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(managers);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<AdminUserDTO> getUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new WorkflowException("User " + id + " not found."));
        return ResponseEntity.ok(toDto(user));
    }

    @GetMapping("/{id}/direct-reports")
    @Transactional(readOnly = true)
    public ResponseEntity<List<AdminUserDTO>> getDirectReports(@PathVariable Long id) {
        userRepository.findById(id)
                .orElseThrow(() -> new WorkflowException("User " + id + " not found."));

        List<AdminUserDTO> reports = userRepository.findByManagerId(id).stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(reports);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<AdminUserDTO> createUser(@Valid @RequestBody AdminCreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername().trim())) {
            throw new WorkflowException("Username '" + request.getUsername().trim() + "' is already taken.");
        }
        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase(Locale.ROOT))) {
            throw new WorkflowException("Email '" + request.getEmail().trim() + "' is already in use.");
        }

        Set<ERole> roles = new HashSet<>();
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            for (String r : request.getRoles()) {
                roles.add(parseRole(r));
            }
        } else {
            roles.add(ERole.ROLE_EMPLOYEE);
        }

        String rawPassword = request.getPassword();
        if (rawPassword == null || rawPassword.isBlank()) {
            rawPassword = UUID.randomUUID().toString().substring(0, 12);
        }
        String encodedPassword = passwordEncoder != null
                ? passwordEncoder.encode(rawPassword)
                : rawPassword;

        User user = new User(request.getUsername().trim(), request.getEmail().trim().toLowerCase(Locale.ROOT), encodedPassword);
        user.setRoles(roles);
        if (request.getDepartment() != null && !request.getDepartment().isBlank()) {
            user.setDepartment(request.getDepartment().trim());
        }
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        if (request.getManagerId() != null) {
            User manager = userRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new WorkflowException("Manager " + request.getManagerId() + " not found."));
            if (!manager.isEnabled()) {
                throw new WorkflowException("Selected manager is inactive.");
            }
            boolean isManager = manager.getRoles().stream().anyMatch(r -> r.name().endsWith("_MANAGER") || r == ERole.ROLE_ADMIN);
            if (!isManager) {
                throw new WorkflowException("Selected user does not have a manager role.");
            }
            user.setManager(manager);
        }

        User saved = userRepository.save(user);
        return ResponseEntity.ok(toDto(saved));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<AdminUserDTO> updateUser(
            @PathVariable Long id,
            @RequestBody AdminUserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new WorkflowException("User " + id + " not found."));

        boolean wasAdmin = user.getRoles().contains(ERole.ROLE_ADMIN);
        boolean deactivating = Boolean.FALSE.equals(request.getEnabled());
        boolean removingAdminRole = false;

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            Set<ERole> newRoles = new HashSet<>();
            for (String roleStr : request.getRoles()) {
                newRoles.add(parseRole(roleStr));
            }
            removingAdminRole = wasAdmin && !newRoles.contains(ERole.ROLE_ADMIN);
            user.setRoles(newRoles);
        }

        // Guard: Prevent deactivating or un-adminning the last active administrator
        if (wasAdmin && (deactivating || removingAdminRole)) {
            long activeAdmins = userRepository.countByRolesContainingAndEnabledTrue(ERole.ROLE_ADMIN);
            if (activeAdmins <= 1) {
                throw new WorkflowException("Cannot deactivate or remove admin role from the last active administrator.");
            }
        }

        if (request.getDepartment() != null) {
            user.setDepartment(request.getDepartment().trim());
        }

        if (request.getManagerId() != null) {
            if (request.getManagerId() == 0L) {
                user.setManager(null);
            } else {
                User manager = userRepository.findById(request.getManagerId())
                        .orElseThrow(() -> new WorkflowException("Manager " + request.getManagerId() + " not found."));
                validateNoCircularReporting(user, manager);
                if (!manager.isEnabled()) {
                    throw new WorkflowException("Selected manager is inactive.");
                }
                boolean isManager = manager.getRoles().stream().anyMatch(r -> r.name().endsWith("_MANAGER") || r == ERole.ROLE_ADMIN);
                if (!isManager) {
                    throw new WorkflowException("Selected user does not have a manager role.");
                }
                user.setManager(manager);
            }
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

    private void validateNoCircularReporting(User employee, User prospectiveManager) {
        if (prospectiveManager.getId().equals(employee.getId())) {
            throw new WorkflowException("A user cannot be their own manager.");
        }
        User current = prospectiveManager;
        Set<Long> visited = new HashSet<>();
        while (current != null) {
            if (current.getId().equals(employee.getId())) {
                throw new WorkflowException("Circular reporting relationship detected: "
                        + prospectiveManager.getUsername() + " already reports to " + employee.getUsername() + ".");
            }
            if (!visited.add(current.getId())) {
                break;
            }
            if (current.getManager() != null) {
                current = current.getManager();
            } else if (current.getManagerId() != null) {
                current = userRepository.findById(current.getManagerId()).orElse(null);
            } else {
                current = null;
            }
        }
    }

    private ERole parseRole(String roleStr) {
        String normalized = roleStr.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (!normalized.startsWith("ROLE_")) {
            normalized = "ROLE_" + normalized;
        }
        try {
            return ERole.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new WorkflowException("Unknown role: " + roleStr);
        }
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
