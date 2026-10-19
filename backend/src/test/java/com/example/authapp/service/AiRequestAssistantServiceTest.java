package com.example.authapp.service;

import com.example.authapp.dto.AiRequestParseResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiRequestAssistantServiceTest {

    private AiRequestAssistantService aiService;

    @BeforeEach
    void setUp() {
        aiService = new AiRequestAssistantService(new ObjectMapper());
    }

    @Test
    @DisplayName("parses budget software request correctly with amount and classification")
    void parsesBudgetSoftwareRequest() {
        String prompt = "I need approval for a department software purchase costing 25,000.";
        AiRequestParseResponseDTO dto = aiService.parseRequest(prompt);

        assertThat(dto.getRequestType()).isEqualTo("BUDGET_REQUEST");
        assertThat(dto.getTitle()).contains("Software");
        assertThat(dto.getExtractedData()).containsKey("amount");
        assertThat(((Number) dto.getExtractedData().get("amount")).doubleValue()).isEqualTo(25000.0);
        assertThat(dto.isFallback()).isTrue();
        assertThat(dto.getMissingFields()).doesNotContain("amount");
        assertThat(dto.getExplanation()).contains("25,000");
    }

    @Test
    @DisplayName("parses urgent sick leave request and flags missing dates")
    void parsesUrgentSickLeaveRequest() {
        String prompt = "Urgent: Need 3 days sick leave due to flu.";
        AiRequestParseResponseDTO dto = aiService.parseRequest(prompt);

        assertThat(dto.getRequestType()).isEqualTo("LEAVE_REQUEST");
        assertThat(dto.getTitle()).contains("Sick Leave");
        assertThat(dto.getPriority()).isEqualTo("HIGH");
        assertThat(dto.getExtractedData().get("days")).isEqualTo(3);
        assertThat(dto.getMissingFields()).contains("startDate", "endDate");
        assertThat(dto.getExplanation()).contains("Sick Leave");
    }

    @Test
    @DisplayName("flags missing amount when budget request omits cost")
    void flagsMissingBudgetAmount() {
        String prompt = "Need to purchase additional developer laptops for Engineering team.";
        AiRequestParseResponseDTO dto = aiService.parseRequest(prompt);

        assertThat(dto.getRequestType()).isEqualTo("BUDGET_REQUEST");
        assertThat(dto.getMissingFields()).contains("amount");
        assertThat(dto.getExtractedData().get("department")).isEqualTo("Engineering");
    }

    @Test
    @DisplayName("rejects blank or empty prompt")
    void rejectsBlankPrompt() {
        assertThatThrownBy(() -> aiService.parseRequest("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Request description cannot be blank");
    }
}
