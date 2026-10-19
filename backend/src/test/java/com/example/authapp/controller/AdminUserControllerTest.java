package com.example.authapp.controller;

import com.example.authapp.dto.AdminUserDTO;
import com.example.authapp.dto.AdminUserUpdateRequest;
import com.example.authapp.entity.ERole;
import com.example.authapp.entity.User;
import com.example.authapp.exception.WorkflowException;
import com.example.authapp.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminUserController adminUserController;

    @Test
    @DisplayName("admin can list all users")
    void adminCanListUsers() {
        User u1 = new User("alice", "alice@co.com", "hash");
        u1.setId(1L);
        u1.setRoles(Set.of(ERole.ROLE_EMPLOYEE));

        User u2 = new User("bob", "bob@co.com", "hash");
        u2.setId(2L);
        u2.setRoles(Set.of(ERole.ROLE_MANAGER));

        when(userRepository.findAll()).thenReturn(List.of(u1, u2));

        ResponseEntity<List<AdminUserDTO>> response = adminUserController.listUsers();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    @DisplayName("admin can update user roles, department, and manager")
    void adminCanUpdateUserRolesAndManager() {
        User user = new User("alice", "alice@co.com", "hash");
        user.setId(1L);
        user.setRoles(Set.of(ERole.ROLE_EMPLOYEE));

        User manager = new User("bob", "bob@co.com", "hash");
        manager.setId(2L);
        manager.setRoles(Set.of(ERole.ROLE_MANAGER));

        AdminUserUpdateRequest request = new AdminUserUpdateRequest();
        request.setRoles(Set.of("ROLE_MANAGER"));
        request.setDepartment("Finance & Accounting");
        request.setManagerId(2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<AdminUserDTO> response = adminUserController.updateUser(1L, request);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        AdminUserDTO dto = response.getBody();
        assertThat(dto.roles()).contains(ERole.ROLE_MANAGER);
        assertThat(dto.department()).isEqualTo("Finance & Accounting");
        assertThat(dto.managerId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("user cannot be set as their own manager")
    void cannotSetSelfAsManager() {
        User user = new User("alice", "alice@co.com", "hash");
        user.setId(1L);

        AdminUserUpdateRequest request = new AdminUserUpdateRequest();
        request.setManagerId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> adminUserController.updateUser(1L, request))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("A user cannot be their own manager");
    }
}
