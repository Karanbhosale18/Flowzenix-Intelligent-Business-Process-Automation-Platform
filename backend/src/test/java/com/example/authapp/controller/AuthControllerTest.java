package com.example.authapp.controller;

import com.example.authapp.entity.ERole;
import com.example.authapp.entity.User;
import com.example.authapp.payload.request.SignupRequest;
import com.example.authapp.payload.response.MessageResponse;
import com.example.authapp.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder encoder;

    @InjectMocks
    private AuthController authController;

    @Test
    @DisplayName("public registration strictly assigns ROLE_EMPLOYEE and ignores privileged role inputs")
    void publicSignupAlwaysAssignsEmployeeRole() {
        SignupRequest request = new SignupRequest();
        request.setUsername("sneaky.user");
        request.setEmail("sneaky@test.com");
        request.setPassword("password123");
        request.setRole(Set.of("ADMIN", "MANAGER"));
        request.setDepartment("Marketing");

        when(userRepository.existsByUsername("sneaky.user")).thenReturn(false);
        when(userRepository.existsByEmail("sneaky@test.com")).thenReturn(false);
        when(encoder.encode(anyString())).thenReturn("hashed-password");

        ResponseEntity<?> response = authController.registerUser(request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(((MessageResponse) response.getBody()).getMessage()).contains("successfully");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();

        assertThat(saved.getRoles()).containsExactly(ERole.ROLE_EMPLOYEE);
        assertThat(saved.getRoles()).doesNotContain(ERole.ROLE_ADMIN);
        assertThat(saved.getDepartment()).isEqualTo("Marketing");
        assertThat(saved.getAdminReferenceId()).isNull();
        assertThat(saved.getManagerReferenceId()).isNull();
    }

    @Test
    @DisplayName("public registration rejects duplicate username")
    void publicSignupRejectsDuplicateUsername() {
        SignupRequest request = new SignupRequest();
        request.setUsername("existing.user");
        request.setEmail("new@test.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("existing.user")).thenReturn(true);

        ResponseEntity<?> response = authController.registerUser(request);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(((MessageResponse) response.getBody()).getMessage()).contains("Username is already taken");
    }

    @Test
    @DisplayName("public registration rejects duplicate email")
    void publicSignupRejectsDuplicateEmail() {
        SignupRequest request = new SignupRequest();
        request.setUsername("new.user");
        request.setEmail("existing@test.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("new.user")).thenReturn(false);
        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);

        ResponseEntity<?> response = authController.registerUser(request);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(((MessageResponse) response.getBody()).getMessage()).contains("Email is already in use");
    }
}
