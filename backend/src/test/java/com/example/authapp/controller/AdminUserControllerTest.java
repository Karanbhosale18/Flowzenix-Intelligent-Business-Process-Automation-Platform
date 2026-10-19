package com.example.authapp.controller;

import com.example.authapp.dto.AdminCreateUserRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;

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

    @Mock
    private PasswordEncoder passwordEncoder;

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

        ResponseEntity<List<AdminUserDTO>> response = adminUserController.listUsers(null, null, null, null);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    @DisplayName("admin can create a new user with hashed password and manager")
    void adminCanCreateUser() {
        User manager = new User("bob", "bob@co.com", "hash");
        manager.setId(2L);
        manager.setRoles(Set.of(ERole.ROLE_MANAGER));
        manager.setEnabled(true);

        AdminCreateUserRequest req = new AdminCreateUserRequest(
                "charlie", "charlie@co.com", "secret123", "Engineering", 2L, Set.of("ROLE_EMPLOYEE")
        );

        when(userRepository.existsByUsername("charlie")).thenReturn(false);
        when(userRepository.existsByEmail("charlie@co.com")).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(passwordEncoder.encode("secret123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(3L);
            return u;
        });

        ResponseEntity<AdminUserDTO> response = adminUserController.createUser(req);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().username()).isEqualTo("charlie");
        assertThat(response.getBody().managerId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("creating user with duplicate email throws exception")
    void duplicateEmailRejected() {
        AdminCreateUserRequest req = new AdminCreateUserRequest(
                "charlie", "exists@co.com", "secret123", "Engineering", null, Set.of("ROLE_EMPLOYEE")
        );

        when(userRepository.existsByUsername("charlie")).thenReturn(false);
        when(userRepository.existsByEmail("exists@co.com")).thenReturn(true);

        assertThatThrownBy(() -> adminUserController.createUser(req))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("already in use");
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

    @Test
    @DisplayName("circular reporting chain A -> B -> C -> A is detected and rejected")
    void circularReportingChainRejected() {
        User userA = new User("alice", "alice@co.com", "hash");
        userA.setId(1L);

        User userB = new User("bob", "bob@co.com", "hash");
        userB.setId(2L);
        userB.setRoles(Set.of(ERole.ROLE_MANAGER));
        userB.setManager(userA); // B reports to A

        User userC = new User("charlie", "charlie@co.com", "hash");
        userC.setId(3L);
        userC.setRoles(Set.of(ERole.ROLE_MANAGER));
        userC.setManager(userB); // C reports to B

        // Trying to set C as A's manager would create: A -> C -> B -> A cycle
        AdminUserUpdateRequest request = new AdminUserUpdateRequest();
        request.setManagerId(3L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(userA));
        when(userRepository.findById(3L)).thenReturn(Optional.of(userC));

        assertThatThrownBy(() -> adminUserController.updateUser(1L, request))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("Circular reporting");
    }

    @Test
    @DisplayName("last active admin cannot be deactivated or have admin role removed")
    void lastActiveAdminProtected() {
        User admin = new User("admin", "admin@co.com", "hash");
        admin.setId(1L);
        admin.setRoles(Set.of(ERole.ROLE_ADMIN));
        admin.setEnabled(true);

        AdminUserUpdateRequest request = new AdminUserUpdateRequest();
        request.setEnabled(false); // attempt to deactivate

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRolesContainingAndEnabledTrue(ERole.ROLE_ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> adminUserController.updateUser(1L, request))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("Cannot deactivate or remove admin role");
    }

    @Test
    @DisplayName("admin can list direct reports for a manager")
    void adminCanListDirectReports() {
        User manager = new User("bob", "bob@co.com", "hash");
        manager.setId(2L);

        User emp1 = new User("alice", "alice@co.com", "hash");
        emp1.setId(1L);
        emp1.setManager(manager);

        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(userRepository.findByManagerId(2L)).thenReturn(List.of(emp1));

        ResponseEntity<List<AdminUserDTO>> res = adminUserController.getDirectReports(2L);
        assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(res.getBody()).hasSize(1);
        assertThat(res.getBody().get(0).username()).isEqualTo("alice");
    }
}
