package com.example.authapp.controller;

import com.example.authapp.payload.request.LoginRequest;
import com.example.authapp.payload.request.SignupRequest;
import com.example.authapp.payload.response.JwtResponse;
import com.example.authapp.payload.response.MessageResponse;
import com.example.authapp.security.jwt.JwtUtils;
import com.example.authapp.security.services.UserDetailsImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthController authController;

    @Test
    @DisplayName("public registration is disabled and returns 403 FORBIDDEN")
    void publicRegistrationIsDisabled() {
        SignupRequest request = new SignupRequest();
        request.setUsername("new.user");
        request.setEmail("user@test.com");
        request.setPassword("password123");

        ResponseEntity<?> response = authController.registerUser(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isInstanceOf(MessageResponse.class);
        MessageResponse message = (MessageResponse) response.getBody();
        assertThat(message.getMessage()).isEqualTo("Public registration is disabled. User accounts are created by administrators.");
    }

    @Test
    @DisplayName("authenticateUser successfully authenticates and returns JWT token with user details")
    void authenticateUserSuccess() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("Admin@123");

        UserDetailsImpl principal = new UserDetailsImpl(
                1L,
                "admin",
                "admin@flowzenix.com",
                "hashed",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtUtils.generateJwtToken(auth)).thenReturn("mock-jwt-token-xyz");

        ResponseEntity<?> response = authController.authenticateUser(loginRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(JwtResponse.class);
        JwtResponse jwtResponse = (JwtResponse) response.getBody();
        assertThat(jwtResponse.getToken()).isEqualTo("mock-jwt-token-xyz");
        assertThat(jwtResponse.getUsername()).isEqualTo("admin");
        assertThat(jwtResponse.getEmail()).isEqualTo("admin@flowzenix.com");
        assertThat(jwtResponse.getRoles()).containsExactly("ROLE_ADMIN");
    }
}
