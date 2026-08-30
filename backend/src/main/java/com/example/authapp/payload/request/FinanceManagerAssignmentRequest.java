package com.example.authapp.payload.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** A manager selects the Finance Manager who receives future budget approvals. */
public class FinanceManagerAssignmentRequest {
    @NotNull
    @Positive
    private Long financeManagerId;

    public Long getFinanceManagerId() { return financeManagerId; }
    public void setFinanceManagerId(Long financeManagerId) { this.financeManagerId = financeManagerId; }
}
