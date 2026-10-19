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
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Authentication", description = "Public registration and JWT login endpoints")
@SecurityRequirements
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
    public ResponseEntity<?> registerUser(@Valid @RequestBody(required = false) SignupRequest signUpRequest) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                .body(new MessageResponse("Public registration is disabled. User accounts are created by administrators."));
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
