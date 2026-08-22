package com.example.authapp.entity;

/**
 * What kind of action a WorkflowStep performs. Only APPROVAL is fully
 * implemented by the engine right now; ACTION and NOTIFICATION are modelled
 * so new step behaviour can be added without changing the schema.
 */
public enum StepType {
    APPROVAL,
    ACTION,
    NOTIFICATION
}
