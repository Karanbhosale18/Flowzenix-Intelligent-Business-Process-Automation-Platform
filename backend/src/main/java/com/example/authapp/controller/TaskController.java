package com.example.authapp.controller;

import com.example.authapp.dto.TaskDecisionDTO;
import com.example.authapp.dto.TaskSummaryDTO;
import com.example.authapp.entity.User;
import com.example.authapp.payload.response.MessageResponse;
import com.example.authapp.repository.UserRepository;
import com.example.authapp.security.services.UserDetailsImpl;
import com.example.authapp.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired private TaskService taskService;
    @Autowired private UserRepository userRepository;

    @GetMapping("/my")
    public ResponseEntity<List<TaskSummaryDTO>> myTasks(@AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(taskService.listMyTasks(currentUser(principal)));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<MessageResponse> approve(
            @PathVariable Long id,
            @RequestBody(required = false) TaskDecisionDTO body,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        taskService.approve(id, currentUser(principal), body != null ? body.getComment() : null);
        return ResponseEntity.ok(new MessageResponse("Task approved."));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<MessageResponse> reject(
            @PathVariable Long id,
            @RequestBody(required = false) TaskDecisionDTO body,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        taskService.reject(id, currentUser(principal), body != null ? body.getComment() : null);
        return ResponseEntity.ok(new MessageResponse("Task rejected."));
    }

    @PostMapping("/{id}/request-information")
    public ResponseEntity<MessageResponse> requestInformation(
            @PathVariable Long id,
            @RequestBody(required = false) TaskDecisionDTO body,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        taskService.requestInformation(id, currentUser(principal), body != null ? body.getComment() : null);
        return ResponseEntity.ok(new MessageResponse("Additional information requested."));
    }

    private User currentUser(UserDetailsImpl principal) {
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists."));
    }
}
