package com.example.authapp.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AiRequestParseResponseDTO {

    private String requestType;
    private String title;
    private String description;
    private String priority;
    private Map<String, Object> extractedData = new HashMap<>();
    private List<String> missingFields = new ArrayList<>();
    private String explanation;
    private double confidenceScore;
    private boolean fallback;

    public AiRequestParseResponseDTO() {}

    public AiRequestParseResponseDTO(String requestType, String title, String description,
                                     String priority, Map<String, Object> extractedData,
                                     List<String> missingFields, String explanation,
                                     double confidenceScore, boolean fallback) {
        this.requestType = requestType;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.extractedData = extractedData != null ? extractedData : new HashMap<>();
        this.missingFields = missingFields != null ? missingFields : new ArrayList<>();
        this.explanation = explanation;
        this.confidenceScore = confidenceScore;
        this.fallback = fallback;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Map<String, Object> getExtractedData() {
        return extractedData;
    }

    public void setExtractedData(Map<String, Object> extractedData) {
        this.extractedData = extractedData != null ? extractedData : new HashMap<>();
    }

    public List<String> getMissingFields() {
        return missingFields;
    }

    public void setMissingFields(List<String> missingFields) {
        this.missingFields = missingFields != null ? missingFields : new ArrayList<>();
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public boolean isFallback() {
        return fallback;
    }

    public void setFallback(boolean fallback) {
        this.fallback = fallback;
    }
}
