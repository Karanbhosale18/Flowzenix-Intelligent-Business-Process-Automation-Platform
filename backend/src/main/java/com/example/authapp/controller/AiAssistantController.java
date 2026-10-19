package com.example.authapp.controller;

import com.example.authapp.dto.AiRequestParseRequest;
import com.example.authapp.dto.AiRequestParseResponseDTO;
import com.example.authapp.service.AiRequestAssistantService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for AI-assisted business request creation.
 * Suggests typed fields, missing info, and workflow categories
 * without bypassing server-side validation or human review.
 */
@RestController
@RequestMapping("/api/ai")
@PreAuthorize("isAuthenticated()")
public class AiAssistantController {

    private final AiRequestAssistantService aiRequestAssistantService;

    public AiAssistantController(AiRequestAssistantService aiRequestAssistantService) {
        this.aiRequestAssistantService = aiRequestAssistantService;
    }

    @PostMapping("/parse-request")
    public ResponseEntity<AiRequestParseResponseDTO> parseRequest(@Valid @RequestBody AiRequestParseRequest request) {
        AiRequestParseResponseDTO response = aiRequestAssistantService.parseRequest(request.getPrompt());
        return ResponseEntity.ok(response);
    }
}
