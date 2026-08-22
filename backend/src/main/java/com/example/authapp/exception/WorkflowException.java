package com.example.authapp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Raised for any workflow-domain problem that isn't a validation error:
 * no active definition for a request type, no assignee could be resolved
 * for a step's role, or someone tries to act on a task that isn't theirs.
 * Kept as one exception type with a status so GlobalExceptionHandler can
 * return it as clean JSON without a stack trace.
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class WorkflowException extends RuntimeException {
    public WorkflowException(String message) {
        super(message);
    }
}
