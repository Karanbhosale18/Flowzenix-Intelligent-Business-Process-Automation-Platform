package com.example.authapp.payload.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public class SignupRequest {

    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank
    @Size(max = 100)
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, max = 120)
    private String password;

    // Optional: allows requesting a role at signup time for demo purposes.
    // In production this should be locked down / removed and roles should be
    // assigned by an existing admin instead of the signup form itself.
    private Set<String> role;

    // Optional: department name, e.g. "Engineering", "Marketing".
    private String department;

    // Optional: id of this person's reporting manager (another user's id).
    // Required for LEAVE_REQUEST-style workflows that route to "the employee's manager".
    private Long managerId;

    private Long adminId;

    private Long adminReferenceId;

    private Long managerReferenceId;

    private Long financeManagerReferenceId;

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

    public Set<String> getRole() {
        return role;
    }

    public void setRole(Set<String> role) {
        this.role = role;
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
}
