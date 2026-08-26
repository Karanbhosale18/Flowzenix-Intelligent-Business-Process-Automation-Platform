package com.example.authapp.controller;

import com.example.authapp.dto.CreateRequestDTO;
import com.example.authapp.dto.RequestDetailDTO;
import com.example.authapp.dto.RequestSummaryDTO;
import com.example.authapp.entity.User;
import com.example.authapp.payload.response.MessageResponse;
import com.example.authapp.repository.UserRepository;
import com.example.authapp.security.services.UserDetailsImpl;
import com.example.authapp.service.RequestService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
public class RequestController {

    @Autowired private RequestService requestService;
    @Autowired private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<RequestSummaryDTO> createRequest(
            @Valid @RequestBody CreateRequestDTO dto,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        User user = currentUser(principal);
        return ResponseEntity.ok(requestService.createRequest(dto, user));
    }

    @GetMapping
    public ResponseEntity<List<RequestSummaryDTO>> listRequests(@AuthenticationPrincipal UserDetailsImpl principal) {
        User user = currentUser(principal);
        return ResponseEntity.ok(requestService.listRequestsFor(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RequestDetailDTO> getRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        User user = currentUser(principal);
        return ResponseEntity.ok(requestService.getRequestDetail(id, user));
    }

    /**
     * Cancels a request. Only the request's owner (or an ADMIN) may cancel,
     * and only while it is still in flight — the service and engine enforce
     * both rules, returning 403 / 422 respectively.
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<MessageResponse> cancelRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        requestService.cancelRequest(id, currentUser(principal));
        return ResponseEntity.ok(new MessageResponse("Request cancelled."));
    }

    private User currentUser(UserDetailsImpl principal) {
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists."));
    }
}
