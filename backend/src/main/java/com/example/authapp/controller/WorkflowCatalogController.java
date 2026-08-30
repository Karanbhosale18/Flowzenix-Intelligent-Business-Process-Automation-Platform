package com.example.authapp.controller;

import com.example.authapp.dto.WorkflowDefinitionResponseDTO;
import com.example.authapp.service.WorkflowAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/** Read-only catalog used by the employee request form. */
@RestController
@RequestMapping("/api/workflows")
public class WorkflowCatalogController {
    private final WorkflowAdminService workflowAdminService;

    public WorkflowCatalogController(WorkflowAdminService workflowAdminService) {
        this.workflowAdminService = workflowAdminService;
    }

    @GetMapping("/active")
    public ResponseEntity<List<WorkflowDefinitionResponseDTO>> active() {
        return ResponseEntity.ok(workflowAdminService.activeCatalog());
    }
}
