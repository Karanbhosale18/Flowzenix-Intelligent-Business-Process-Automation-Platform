package com.example.authapp.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class WorkflowReorderDTO {
    @NotEmpty
    private List<Long> stepIds;

    public List<Long> getStepIds() { return stepIds; }
    public void setStepIds(List<Long> stepIds) { this.stepIds = stepIds; }
}
