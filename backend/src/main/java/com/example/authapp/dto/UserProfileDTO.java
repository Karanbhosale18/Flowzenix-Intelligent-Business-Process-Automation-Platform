package com.example.authapp.dto;

public record UserProfileDTO(Long id, String username, String email, String department,
                             Long managerId, String managerName, Long adminReferenceId,
                             Long managerReferenceId, Long assignedManagerReferenceId) {}
