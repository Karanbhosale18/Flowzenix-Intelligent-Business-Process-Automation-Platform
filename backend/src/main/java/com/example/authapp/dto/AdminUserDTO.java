package com.example.authapp.dto;

import com.example.authapp.entity.ERole;
import java.util.Set;

public record AdminUserDTO(
        Long id,
        String username,
        String email,
        Set<ERole> roles,
        String department,
        Long managerId,
        String managerName,
        Long adminId,
        Long adminReferenceId,
        Long managerReferenceId,
        Long financeManagerReferenceId,
        Long financeManagerId,
        boolean enabled
) {}
