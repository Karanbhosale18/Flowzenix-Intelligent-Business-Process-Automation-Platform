package com.example.authapp.service;

import com.example.authapp.dto.AiRequestParseResponseDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI-assisted request parser that classifies business requests into supported
 * workflow types, extracts structured fields, explains missing or ambiguous data,
 * and seamlessly falls back to local rule-based NLP extraction when the AI provider
 * is offline, unconfigured, or times out.
 */
@Service
public class AiRequestAssistantService {

    private static final Logger log = LoggerFactory.getLogger(AiRequestAssistantService.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${app.ai.enabled:false}")
    private boolean aiEnabled;

    @Value("${app.ai.api-key:}")
    private String apiKey;

    @Value("${app.ai.model:gpt-4o-mini}")
    private String model;

    @Value("${app.ai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${app.ai.timeout-ms:5000}")
    private int timeoutMs;

    public AiRequestAssistantService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public AiRequestParseResponseDTO parseRequest(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Request description cannot be blank.");
        }

        String trimmed = prompt.trim();

        if (aiEnabled && apiKey != null && !apiKey.isBlank()) {
            try {
                return callAiProvider(trimmed);
            } catch (Exception e) {
                log.warn("Remote AI provider call failed ({}), falling back to local heuristic extraction: {}",
                        e.getClass().getSimpleName(), e.getMessage());
            }
        }

        return parseWithRules(trimmed);
    }

    private AiRequestParseResponseDTO callAiProvider(String prompt) throws Exception {
        String systemPrompt = """
            You are a business process assistant for Flowzenix. Classify this request into either 'BUDGET' or 'LEAVE'.
            Extract the following fields into JSON:
            {
              "requestType": "BUDGET" or "LEAVE",
              "title": "concise business title",
              "description": "clear summary",
              "priority": "LOW" | "MEDIUM" | "HIGH" | "CRITICAL",
              "extractedData": {
                 // For BUDGET: amount (number), department, vendor or purpose
                 // For LEAVE: leaveType, startDate, endDate, days
              },
              "missingFields": ["list of essential fields not specified by user"],
              "explanation": "concise explanation of what was extracted and what is missing",
              "confidenceScore": 0.0 to 1.0
            }
            Output only raw valid JSON without markdown wrapping.
            """;

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.1
        );

        String jsonBody = objectMapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl.replaceAll("/+$", "") + "/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofMillis(timeoutMs))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText();
            if (content != null && !content.isBlank()) {
                // Strip markdown code fences if present
                content = content.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
                AiRequestParseResponseDTO dto = objectMapper.readValue(content, AiRequestParseResponseDTO.class);
                dto.setFallback(false);
                return dto;
            }
        }
        throw new RuntimeException("AI provider returned status code " + response.statusCode());
    }

    /**
     * Intelligent rule-based NLP extraction when AI is disabled or offline.
     */
    public AiRequestParseResponseDTO parseWithRules(String prompt) {
        String lower = prompt.toLowerCase(Locale.ROOT);

        boolean mentionsLeave = lower.contains("leave") || lower.contains("vacation") || lower.contains("sick")
                || lower.contains("pto") || lower.contains("holiday") || lower.contains("time off")
                || lower.contains("absence");

        boolean mentionsBudget = lower.contains("budget") || lower.contains("purchase") || lower.contains("cost")
                || lower.contains("software") || lower.contains("license") || lower.contains("hardware")
                || lower.contains("equipment") || lower.contains("expense") || lower.contains("dollar")
                || lower.contains("$") || lower.contains("price") || lower.contains("buy");

        String requestType = (mentionsBudget || !mentionsLeave) ? "BUDGET_REQUEST" : "LEAVE_REQUEST";

        // Determine priority
        String priority = "MEDIUM";
        if (lower.contains("urgent") || lower.contains("asap") || lower.contains("emergency") || lower.contains("critical")) {
            priority = "HIGH";
        } else if (lower.contains("low priority") || lower.contains("when possible")) {
            priority = "LOW";
        }

        Map<String, Object> extractedData = new HashMap<>();
        List<String> missingFields = new ArrayList<>();
        String title;
        String explanation;
        double confidence = 0.85;

        if ("BUDGET_REQUEST".equals(requestType)) {
            // Extract amount
            Pattern amountPattern = Pattern.compile("(?:costing|cost|amount|for|of|price|\\$)?\\s*\\$?\\s*(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{1,2})?|\\d+(?:\\.\\d{1,2})?)");
            Matcher m = amountPattern.matcher(prompt);
            Double amount = null;
            while (m.find()) {
                String match = m.group(1);
                if (match != null && !match.isBlank()) {
                    try {
                        double parsed = Double.parseDouble(match.replace(",", ""));
                        if (parsed > 0) {
                            amount = parsed;
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }

            if (amount != null) {
                extractedData.put("amount", amount);
            } else {
                missingFields.add("amount");
            }

            // Department detection for canonical Flowzenix departments
            if (lower.contains("research & development") || lower.contains("research") || lower.contains("r&d") || lower.contains("engineering")) {
                extractedData.put("department", "Research & Development");
            } else if (lower.contains("finance & accounting") || lower.contains("finance") || lower.contains("accounting")) {
                extractedData.put("department", "Finance & Accounting");
            } else if (lower.contains("sales & business development") || lower.contains("business development") || lower.contains("sales")) {
                extractedData.put("department", "Sales & Business Development");
            } else if (lower.contains("technical support") || lower.contains("help desk") || lower.contains("support")) {
                extractedData.put("department", "Technical Support / Help Desk");
            } else if (lower.contains("marketing")) {
                extractedData.put("department", "Marketing");
            } else if (lower.contains("admin")) {
                extractedData.put("department", "Admin");
            }

            if (lower.contains("software")) {
                title = "Software Purchase Approval";
                extractedData.put("purpose", "Software purchase & licenses");
            } else if (lower.contains("hardware") || lower.contains("equipment") || lower.contains("laptop")) {
                title = "Hardware / Equipment Purchase Approval";
                extractedData.put("purpose", "Hardware & IT equipment");
            } else {
                title = "Department Budget Approval Request";
                extractedData.put("purpose", prompt);
            }

            if (amount != null) {
                explanation = String.format("Classified as Budget Request for '%s' with detected amount of $%,.2f.",
                        extractedData.get("purpose"), amount);
            } else {
                confidence = 0.70;
                explanation = "Classified as Budget Request. An amount could not be detected; please enter the estimated amount before submitting.";
            }
        } else {
            // LEAVE_REQUEST
            String leaveType = "Annual Leave";
            if (lower.contains("sick")) {
                leaveType = "Sick Leave";
            } else if (lower.contains("parental") || lower.contains("maternity") || lower.contains("paternity")) {
                leaveType = "Parental Leave";
            } else if (lower.contains("bereavement")) {
                leaveType = "Bereavement Leave";
            }
            extractedData.put("reason", leaveType + ": " + prompt);

            // Extract days count
            Pattern daysPattern = Pattern.compile("(\\d+)\\s*(?:days|day|business days)");
            Matcher daysMatcher = daysPattern.matcher(lower);
            if (daysMatcher.find()) {
                extractedData.put("days", Integer.parseInt(daysMatcher.group(1)));
            } else {
                missingFields.add("days");
            }

            missingFields.add("startDate");
            missingFields.add("endDate");

            title = leaveType + " Request";
            explanation = String.format("Classified as %s. Specific start and end dates need to be confirmed before submitting.", leaveType);
        }

        return new AiRequestParseResponseDTO(
                requestType,
                title,
                prompt,
                priority,
                extractedData,
                missingFields,
                explanation,
                confidence,
                true
        );
    }
}
