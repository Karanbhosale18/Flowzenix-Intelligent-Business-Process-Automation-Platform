package com.example.authapp.dto;

import java.util.Map;

public class ProvideInformationDTO {
    private String comment;
    private Map<String, Object> metadata;

    public ProvideInformationDTO() {
    }

    public ProvideInformationDTO(String comment) {
        this.comment = comment;
    }

    public ProvideInformationDTO(String comment, Map<String, Object> metadata) {
        this.comment = comment;
        this.metadata = metadata;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
