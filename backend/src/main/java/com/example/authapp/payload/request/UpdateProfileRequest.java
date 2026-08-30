package com.example.authapp.payload.request;

import jakarta.validation.constraints.Size;

/** Fields a signed-in user may maintain for their own account. */
public class UpdateProfileRequest {
    @Size(min = 8, max = 120)
    private String password;
    private String department;
    private Long managerId;

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public Long getManagerId() { return managerId; }
    public void setManagerId(Long managerId) { this.managerId = managerId; }
}
