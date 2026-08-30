package com.example.authapp.controller;

import com.example.authapp.dto.WorkflowDefinitionResponseDTO;
import com.example.authapp.dto.WorkflowReorderDTO;
import com.example.authapp.dto.WorkflowStepRequestDTO;
import com.example.authapp.dto.WorkflowStepResponseDTO;
import com.example.authapp.service.WorkflowAdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** ADMIN-only nested workflow step management. */
@RestController
@RequestMapping("/api/admin/workflows/{workflowId}/steps")
@PreAuthorize("hasRole('ADMIN')")
public class WorkflowStepController {
    private final WorkflowAdminService workflowAdminService;

    public WorkflowStepController(WorkflowAdminService workflowAdminService) {
        this.workflowAdminService = workflowAdminService;
    }

    @PostMapping
    public ResponseEntity<WorkflowStepResponseDTO> add(
            @PathVariable Long workflowId, @Valid @RequestBody WorkflowStepRequestDTO dto) {
        return ResponseEntity.ok(workflowAdminService.addStep(workflowId, dto));
    }

    @PutMapping("/{stepId}")
    public ResponseEntity<WorkflowStepResponseDTO> update(
            @PathVariable Long workflowId, @PathVariable Long stepId,
            @Valid @RequestBody WorkflowStepRequestDTO dto) {
        return ResponseEntity.ok(workflowAdminService.updateStep(workflowId, stepId, dto));
    }

    @DeleteMapping("/{stepId}")
    public ResponseEntity<Void> delete(@PathVariable Long workflowId, @PathVariable Long stepId) {
        workflowAdminService.deleteStep(workflowId, stepId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    public ResponseEntity<WorkflowDefinitionResponseDTO> reorder(
            @PathVariable Long workflowId, @Valid @RequestBody WorkflowReorderDTO dto) {
        return ResponseEntity.ok(workflowAdminService.reorder(workflowId, dto));
    }
}
