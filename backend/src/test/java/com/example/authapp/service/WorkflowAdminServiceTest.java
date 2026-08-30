package com.example.authapp.service;

import com.example.authapp.dto.WorkflowDefinitionRequestDTO;
import com.example.authapp.dto.WorkflowStepRequestDTO;
import com.example.authapp.entity.*;
import com.example.authapp.exception.WorkflowException;
import com.example.authapp.repository.WorkflowDefinitionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowAdminServiceTest {
    @Mock WorkflowDefinitionRepository repository;

    @Test
    void activationRejectsWorkflowWithoutSteps() {
        WorkflowDefinition definition = new WorkflowDefinition("IT support", "", "IT_SUPPORT_REQUEST");
        definition.setId(1L);
        when(repository.findById(1L)).thenReturn(java.util.Optional.of(definition));
        WorkflowAdminService service = new WorkflowAdminService(repository);

        assertThatThrownBy(() -> service.activate(1L))
                .isInstanceOf(WorkflowException.class).hasMessageContaining("at least one step");
    }

    @Test
    void duplicateTypeIsRejectedCaseInsensitively() {
        WorkflowDefinition existing = new WorkflowDefinition("Existing", "", "IT_SUPPORT_REQUEST");
        existing.setId(4L);
        when(repository.findAll()).thenReturn(List.of(existing));
        WorkflowDefinitionRequestDTO dto = definitionRequest("it_support_request", true);
        WorkflowAdminService service = new WorkflowAdminService(repository);

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(WorkflowException.class).hasMessageContaining("already in use");
    }

    @Test
    void activationRejectsNonConsecutiveStepOrder() {
        WorkflowDefinition definition = new WorkflowDefinition("IT support", "", "IT_SUPPORT_REQUEST");
        definition.setId(1L);
        definition.getSteps().add(step(definition, 2));
        when(repository.findById(1L)).thenReturn(java.util.Optional.of(definition));
        WorkflowAdminService service = new WorkflowAdminService(repository);

        assertThatThrownBy(() -> service.activate(1L))
                .isInstanceOf(WorkflowException.class).hasMessageContaining("consecutively");
    }

    @Test
    void invalidAssignedRoleIsRejected() {
        WorkflowDefinitionRequestDTO dto = definitionRequest("IT_SUPPORT_REQUEST", true);
        dto.getSteps().get(0).setAssignedRole("ROLE_NOT_A_REAL_ROLE");
        when(repository.findAll()).thenReturn(List.of());
        WorkflowAdminService service = new WorkflowAdminService(repository);

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(WorkflowException.class).hasMessageContaining("Unknown assigned role");
    }

    private static WorkflowDefinitionRequestDTO definitionRequest(String type, boolean active) {
        WorkflowDefinitionRequestDTO dto = new WorkflowDefinitionRequestDTO();
        dto.setName("IT support"); dto.setWorkflowType(type); dto.setActive(active);
        WorkflowStepRequestDTO step = new WorkflowStepRequestDTO();
        step.setName("Triage"); step.setStepType("APPROVAL"); step.setAssignedRole("IT_ADMIN");
        dto.setSteps(List.of(step));
        return dto;
    }

    private static WorkflowStep step(WorkflowDefinition definition, int order) {
        return new WorkflowStep(definition, order, "Triage", StepType.APPROVAL, ERole.ROLE_IT_ADMIN, true);
    }
}
