package com.example.authapp.controller;

import com.example.authapp.entity.ERole;
import com.example.authapp.entity.User;
import com.example.authapp.payload.request.LoginRequest;
import com.example.authapp.payload.request.SignupRequest;
import com.example.authapp.payload.response.JwtResponse;
import com.example.authapp.payload.response.MessageResponse;
import com.example.authapp.repository.UserRepository;
import com.example.authapp.security.jwt.JwtUtils;
import com.example.authapp.security.services.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Set<String> ALLOWED_DEPARTMENTS = Set.of(
            "Research & Development", "Finance & Accounting", "Marketing",
            "Sales & Business Development", "Technical Support / Help Desk", "Admin");
    private static final Map<String, Set<String>> DEPARTMENT_ROLES = Map.of(
            "Research & Development", Set.of("RND_MANAGER", "RND_ENGINEER", "RND_ANALYST"),
            "Finance & Accounting", Set.of("FINANCE_MANAGER", "ACCOUNTANT", "FINANCE_ANALYST"),
            "Marketing", Set.of("MARKETING_MANAGER", "MARKETING_SPECIALIST", "MARKETING_ANALYST"),
            "Sales & Business Development", Set.of("SALES_MANAGER", "SALES_EXECUTIVE", "BUSINESS_DEVELOPMENT_MANAGER", "ACCOUNT_MANAGER"),
            "Technical Support / Help Desk", Set.of("SUPPORT_MANAGER", "SUPPORT_ENGINEER", "HELP_DESK_AGENT", "SYSTEM_ADMIN"),
            "Admin", Set.of("ADMIN"));

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (Boolean.TRUE.equals(userRepository.existsByUsername(signUpRequest.getUsername()))) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken."));
        }

        if (Boolean.TRUE.equals(userRepository.existsByEmail(signUpRequest.getEmail()))) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use."));
        }

        if (!ALLOWED_DEPARTMENTS.contains(signUpRequest.getDepartment())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Select one of the available departments."));
        }
        if (signUpRequest.getRole() == null || signUpRequest.getRole().size() != 1) {
            return ResponseEntity.badRequest().body(new MessageResponse("Select exactly one of the available roles."));
        }
        String selectedRole = signUpRequest.getRole().iterator().next().toUpperCase();
        if (!DEPARTMENT_ROLES.get(signUpRequest.getDepartment()).contains(selectedRole)) {
            return ResponseEntity.badRequest().body(new MessageResponse("Select a role available for the selected department."));
        }
        boolean isAdmin = "ADMIN".equals(selectedRole);
        boolean isManager = selectedRole.endsWith("_MANAGER");
        if (isAdmin && (signUpRequest.getAdminReferenceId() == null || signUpRequest.getAdminReferenceId() < 1
                || userRepository.existsByAdminReferenceId(signUpRequest.getAdminReferenceId()))) {
            return ResponseEntity.badRequest().body(new MessageResponse("Choose a unique Admin ID."));
        }
        if (isManager && (signUpRequest.getManagerReferenceId() == null || signUpRequest.getManagerReferenceId() < 1
                || userRepository.existsByManagerReferenceId(signUpRequest.getManagerReferenceId()))) {
            return ResponseEntity.badRequest().body(new MessageResponse("Choose a unique Manager ID."));
        }
        if (!isAdmin && !isManager) {
            User manager = signUpRequest.getManagerId() == null ? null : userRepository.findByManagerReferenceId(signUpRequest.getManagerId()).orElse(null);
            boolean validManager = manager != null
                    && manager.getRoles().stream().anyMatch(role -> role.name().endsWith("_MANAGER"))
                    && signUpRequest.getDepartment().equals(manager.getDepartment());
            if (!validManager) return ResponseEntity.badRequest().body(new MessageResponse("Invalid manager ID."));
            signUpRequest.setManagerId(manager.getId());
        }
        if (isManager) {
            User admin = signUpRequest.getAdminId() == null ? null : userRepository.findByAdminReferenceId(signUpRequest.getAdminId()).orElse(null);
            if (admin == null || !admin.getRoles().contains(ERole.ROLE_ADMIN)) {
                return ResponseEntity.badRequest().body(new MessageResponse("Invalid admin ID."));
            }
        }

        // Create the user, hashing the password with BCrypt before it ever touches the DB.
        User user = new User(
                signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword())
        );

        Set<ERole> roles = new HashSet<>();

        roles.add(isAdmin ? ERole.ROLE_ADMIN : ERole.valueOf("ROLE_" + selectedRole));
        // Finance manager remains eligible for pre-existing Finance workflow steps.
        if ("FINANCE_MANAGER".equals(selectedRole)) roles.add(ERole.ROLE_FINANCE);

        user.setRoles(roles);
        user.setDepartment(signUpRequest.getDepartment());
        user.setManagerId(signUpRequest.getManagerId());
        user.setAdminId(signUpRequest.getAdminId());
        if (isAdmin) user.setAdminReferenceId(signUpRequest.getAdminReferenceId());
        if (isManager) user.setManagerReferenceId(signUpRequest.getManagerReferenceId());
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        // Delegates to the DaoAuthenticationProvider configured in WebSecurityConfig,
        // which loads the user via UserDetailsServiceImpl and checks the BCrypt hash.
        // Bad credentials throw AuthenticationException, handled by GlobalExceptionHandler.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return ResponseEntity.ok(new JwtResponse(
                jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles
        ));
    }
}
