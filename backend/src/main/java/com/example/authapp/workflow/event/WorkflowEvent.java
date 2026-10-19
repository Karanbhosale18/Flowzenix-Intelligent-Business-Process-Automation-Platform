package com.example.authapp.workflow.event;

import com.example.authapp.entity.User;
import com.example.authapp.entity.WorkflowInstance;
import com.example.authapp.entity.WorkflowTask;

/**
 * Domain event published when workflow transitions occur.
 * Handled asynchronously / after commit to trigger email notifications.
 */
public class WorkflowEvent {

    public enum EventType {
        SUBMITTED,
        APPROVED,
        REJECTED,
        INFO_REQUESTED,
        INFO_PROVIDED,
        CANCELLED
    }

    private final EventType eventType;
    private final WorkflowInstance instance;
    private final WorkflowTask task;
    private final User actor;
    private final String comment;

    public WorkflowEvent(EventType eventType, WorkflowInstance instance, WorkflowTask task, User actor, String comment) {
        this.eventType = eventType;
        this.instance = instance;
        this.task = task;
        this.actor = actor;
        this.comment = comment;
    }

    public EventType getEventType() {
        return eventType;
    }

    public WorkflowInstance getInstance() {
        return instance;
    }

    public WorkflowTask getTask() {
        return task;
    }

    public User getActor() {
        return actor;
    }

    public String getComment() {
        return comment;
    }
}
