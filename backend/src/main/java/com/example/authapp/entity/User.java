package com.example.authapp.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email")
    }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false)
    private String username;

    @NotBlank
    @Size(max = 100)
    @Email
    @Column(nullable = false)
    private String email;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String password; // stored as a BCrypt hash, never plaintext

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Set<ERole> roles = new HashSet<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private boolean enabled = true;

    @Size(max = 100)
    private String department;

    /**
     * References users.id of this person's reporting manager. Plain scalar
     * column rather than a mapped @ManyToOne — the workflow engine only
     * ever needs to look the manager up by id, and this avoids a
     * self-referencing entity relationship for a single use case.
     */
    private Long managerId;

    /** Admin responsible for this manager's requests. */
    private Long adminId;

    /** User-chosen, memorable identifier used when assigning managers to an Admin. */
    @Column(unique = true)
    private Long adminReferenceId;

    /** User-chosen identifier staff use to connect to their manager. */
    @Column(unique = true)
    private Long managerReferenceId;

    /** User-chosen identifier managers use to delegate budget approvals to a Finance Manager. */
    @Column(unique = true)
    private Long financeManagerReferenceId;

    /**
     * Finance Manager selected by this manager for their team's budget
     * requests. This is a user id and is intentionally maintained by the
     * manager, not inferred from whichever Finance user was created first.
     */
    private Long financeManagerId;

    public User() {
    }

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    // ---------- getters / setters ----------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<ERole> getRoles() {
        return roles;
    }

    public void setRoles(Set<ERole> roles) {
        this.roles = roles;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Long getManagerId() {
        return managerId;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
    }

    public Long getAdminId() { return adminId; }
    public void setAdminId(Long adminId) { this.adminId = adminId; }

    public Long getAdminReferenceId() { return adminReferenceId; }
    public void setAdminReferenceId(Long adminReferenceId) { this.adminReferenceId = adminReferenceId; }

    public Long getManagerReferenceId() { return managerReferenceId; }
    public void setManagerReferenceId(Long managerReferenceId) { this.managerReferenceId = managerReferenceId; }

    public Long getFinanceManagerReferenceId() { return financeManagerReferenceId; }
    public void setFinanceManagerReferenceId(Long financeManagerReferenceId) { this.financeManagerReferenceId = financeManagerReferenceId; }

    public Long getFinanceManagerId() { return financeManagerId; }
    public void setFinanceManagerId(Long financeManagerId) { this.financeManagerId = financeManagerId; }
}
