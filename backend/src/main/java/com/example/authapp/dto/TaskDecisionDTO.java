package com.example.authapp.dto;

/** Body for approve/reject/request-information on a task. */
public class TaskDecisionDTO {
    private String comment;

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
