package com.example.authapp.service;

import com.example.authapp.entity.User;
import com.example.authapp.entity.WorkflowInstance;
import com.example.authapp.entity.WorkflowTask;
import com.example.authapp.workflow.event.WorkflowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Handles workflow notifications. Runs strictly AFTER_COMMIT and wraps all
 * mail operations in a fail-safe try-catch, ensuring email delivery issues
 * NEVER cause rollback or disruption of committed workflow decisions.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:noreply@flowzenix.com}")
    private String fromAddress;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWorkflowEvent(WorkflowEvent event) {
        if (!mailEnabled || mailSender == null) {
            log.debug("Workflow event notification skipped (mailEnabled={}, mailSenderPresent={})",
                    mailEnabled, mailSender != null);
            return;
        }

        try {
            sendNotification(event);
        } catch (Exception ex) {
            // Notification failures must NEVER reverse committed workflow transactions.
            log.warn("Failed to send workflow notification email for event {}: {}", event.getEventType(), ex.getMessage());
        }
    }

    private void sendNotification(WorkflowEvent event) {
        WorkflowInstance instance = event.getInstance();
        String wfName = instance.getWorkflowDefinition() != null
                ? instance.getWorkflowDefinition().getName()
                : "Request";

        switch (event.getEventType()) {
            case SUBMITTED -> {
                WorkflowTask task = event.getTask();
                if (task != null && task.getAssignedTo() != null && task.getAssignedTo().getEmail() != null) {
                    sendEmail(task.getAssignedTo().getEmail(),
                            "[Flowzenix] New Approval Task: " + wfName,
                            String.format("Hello %s,%n%nA new %s has been submitted by %s and requires your approval.%n%nFlowzenix Platform",
                                    task.getAssignedTo().getUsername(), wfName, event.getActor().getUsername()));
                }
            }
            case APPROVED -> {
                User requester = instance.getCreatedBy();
                if (requester != null && requester.getEmail() != null) {
                    sendEmail(requester.getEmail(),
                            "[Flowzenix] " + wfName + " Approved",
                            String.format("Hello %s,%n%nYour %s has been approved by %s.%s%n%nFlowzenix Platform",
                                    requester.getUsername(), wfName, event.getActor().getUsername(),
                                    event.getComment() != null ? "\nComment: " + event.getComment() : ""));
                }
            }
            case REJECTED -> {
                User requester = instance.getCreatedBy();
                if (requester != null && requester.getEmail() != null) {
                    sendEmail(requester.getEmail(),
                            "[Flowzenix] " + wfName + " Rejected",
                            String.format("Hello %s,%n%nYour %s has been rejected by %s.%s%n%nFlowzenix Platform",
                                    requester.getUsername(), wfName, event.getActor().getUsername(),
                                    event.getComment() != null ? "\nReason: " + event.getComment() : ""));
                }
            }
            case INFO_REQUESTED -> {
                User requester = instance.getCreatedBy();
                if (requester != null && requester.getEmail() != null) {
                    sendEmail(requester.getEmail(),
                            "[Flowzenix] Additional Information Requested: " + wfName,
                            String.format("Hello %s,%n%n%s requested additional information regarding your %s:%s%n%nPlease log in to Flowzenix to respond.%n%nFlowzenix Platform",
                                    requester.getUsername(), event.getActor().getUsername(), wfName,
                                    event.getComment() != null ? "\nRequest: " + event.getComment() : ""));
                }
            }
            case INFO_PROVIDED -> {
                WorkflowTask task = event.getTask();
                if (task != null && task.getAssignedTo() != null && task.getAssignedTo().getEmail() != null) {
                    sendEmail(task.getAssignedTo().getEmail(),
                            "[Flowzenix] Information Provided: " + wfName,
                            String.format("Hello %s,%n%n%s has provided the requested information for %s:%s%n%nThe task is now ready for your review.%n%nFlowzenix Platform",
                                    task.getAssignedTo().getUsername(), event.getActor().getUsername(), wfName,
                                    event.getComment() != null ? "\nResponse: " + event.getComment() : ""));
                }
            }
            case CANCELLED -> {
                WorkflowTask task = event.getTask();
                if (task != null && task.getAssignedTo() != null && task.getAssignedTo().getEmail() != null) {
                    sendEmail(task.getAssignedTo().getEmail(),
                            "[Flowzenix] Request Cancelled: " + wfName,
                            String.format("Hello %s,%n%nThe %s previously assigned to you was cancelled by %s.%n%nFlowzenix Platform",
                                    task.getAssignedTo().getUsername(), wfName, event.getActor().getUsername()));
                }
            }
        }
    }

    private void sendEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
        log.info("Sent workflow notification email to {} with subject '{}'", to, subject);
    }
}
