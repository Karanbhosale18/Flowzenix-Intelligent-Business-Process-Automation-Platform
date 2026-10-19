package com.example.authapp.dto;

import java.util.Set;

public class AdminUserUpdateRequest {
    private Set<String> roles;
    private String department;
    private Long managerId;
    private Long adminId;
    private Long adminReferenceId;
    private Long managerReferenceId;
    private Long financeManagerReferenceId;
    private Boolean enabled;

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
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

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public Long getAdminReferenceId() {
        return adminReferenceId;
    }

    public void setAdminReferenceId(Long adminReferenceId) {
        this.adminReferenceId = adminReferenceId;
    }

    public Long getManagerReferenceId() {
        return managerReferenceId;
    }

    public void setManagerReferenceId(Long managerReferenceId) {
        this.managerReferenceId = managerReferenceId;
    }

    public Long getFinanceManagerReferenceId() {
        return financeManagerReferenceId;
    }

    public void setFinanceManagerReferenceId(Long financeManagerReferenceId) {
        this.financeManagerReferenceId = financeManagerReferenceId;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
