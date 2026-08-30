package com.example.authapp.controller;

import com.example.authapp.dto.WorkflowDefinitionRequestDTO;
import com.example.authapp.dto.WorkflowDefinitionResponseDTO;
import com.example.authapp.service.WorkflowAdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** ADMIN-only workflow definition management. */
@RestController
@RequestMapping("/api/admin/workflows")
@PreAuthorize("hasRole('ADMIN')")
public class WorkflowDefinitionController {
    private final WorkflowAdminService workflowAdminService;

    public WorkflowDefinitionController(WorkflowAdminService workflowAdminService) {
        this.workflowAdminService = workflowAdminService;
    }

    @GetMapping
    public ResponseEntity<List<WorkflowDefinitionResponseDTO>> getAll(
            @RequestParam(defaultValue = "true") boolean includeInactive,
            @RequestParam(required = false) Boolean active) {
        // `active` is a convenient explicit filter; includeInactive remains
        // the backwards-compatible default used by the builder.
        return ResponseEntity.ok(active == null
                ? workflowAdminService.list(includeInactive)
                : workflowAdminService.listByActive(active));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkflowDefinitionResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(workflowAdminService.get(id));
    }

    @PostMapping
    public ResponseEntity<WorkflowDefinitionResponseDTO> create(
            @Valid @RequestBody WorkflowDefinitionRequestDTO dto) {
        return ResponseEntity.ok(workflowAdminService.create(dto));
    }

    @PutMapping("/{id}")
    @PatchMapping("/{id}")
    public ResponseEntity<WorkflowDefinitionResponseDTO> update(
            @PathVariable Long id, @Valid @RequestBody WorkflowDefinitionRequestDTO dto) {
        return ResponseEntity.ok(workflowAdminService.update(id, dto));
    }

    /** Soft delete/deactivation preserves definitions referenced by old instances. */
    @DeleteMapping("/{id}")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        workflowAdminService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<WorkflowDefinitionResponseDTO> setActive(
            @PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(active
                ? workflowAdminService.activate(id)
                : (workflowAdminService.deactivateAndGet(id)));
    }

    @PostMapping("/{id}/activate")
    @PutMapping("/{id}/activate")
    public ResponseEntity<WorkflowDefinitionResponseDTO> activate(@PathVariable Long id) {
        return ResponseEntity.ok(workflowAdminService.activate(id));
    }

    @PostMapping("/{id}/deactivate")
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<WorkflowDefinitionResponseDTO> deactivateAndGet(@PathVariable Long id) {
        return ResponseEntity.ok(workflowAdminService.deactivateAndGet(id));
    }
}
