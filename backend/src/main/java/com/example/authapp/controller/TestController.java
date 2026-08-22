package com.example.authapp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sample endpoints to demonstrate that the SecurityFilterChain is enforcing
 * authentication + role-based authorization correctly. Wire your real
 * workflow-automation endpoints (requests, approvals, etc.) alongside these.
 */
@RestController
@RequestMapping("/api")
public class TestController {

    @GetMapping("/public/ping")
    public String publicPing() {
        return "pong (no auth required)";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
        return "This is an ADMIN-only protected endpoint.";
    }
}
