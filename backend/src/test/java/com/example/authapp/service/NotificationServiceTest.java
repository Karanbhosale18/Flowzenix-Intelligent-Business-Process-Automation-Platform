package com.example.authapp.service;

import com.example.authapp.entity.*;
import com.example.authapp.workflow.event.WorkflowEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("Notification is skipped when mail is disabled")
    void skippedWhenMailDisabled() {
        ReflectionTestUtils.setField(notificationService, "mailEnabled", false);
        ReflectionTestUtils.setField(notificationService, "fromAddress", "noreply@flowzenix.com");

        WorkflowInstance instance = sampleInstance();
        WorkflowTask task = new WorkflowTask(instance, instance.getWorkflowDefinition().getSteps().get(0), instance.getCreatedBy());
        WorkflowEvent event = new WorkflowEvent(WorkflowEvent.EventType.SUBMITTED, instance, task, instance.getCreatedBy(), null);

        notificationService.onWorkflowEvent(event);

        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("Sends email notification on request submission")
    void sendsEmailOnSubmission() {
        ReflectionTestUtils.setField(notificationService, "mailEnabled", true);
        ReflectionTestUtils.setField(notificationService, "fromAddress", "noreply@flowzenix.com");

        User approver = new User("approver", "approver@example.com", "pass");
        WorkflowInstance instance = sampleInstance();
        WorkflowTask task = new WorkflowTask(instance, instance.getWorkflowDefinition().getSteps().get(0), approver);

        WorkflowEvent event = new WorkflowEvent(WorkflowEvent.EventType.SUBMITTED, instance, task, instance.getCreatedBy(), null);

        notificationService.onWorkflowEvent(event);

        ArgumentCaptor<SimpleMailMessage> msgCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(msgCaptor.capture());
        SimpleMailMessage msg = msgCaptor.getValue();
        assertThat(msg.getTo()).containsExactly("approver@example.com");
        assertThat(msg.getSubject()).contains("New Approval Task");
    }

    @Test
    @DisplayName("Notification failure does not throw exception or disrupt workflow transaction")
    void mailFailureIsHandledGracefully() {
        ReflectionTestUtils.setField(notificationService, "mailEnabled", true);
        ReflectionTestUtils.setField(notificationService, "fromAddress", "noreply@flowzenix.com");

        doThrow(new MailSendException("SMTP server unreachable")).when(mailSender).send(any(SimpleMailMessage.class));

        User approver = new User("approver", "approver@example.com", "pass");
        WorkflowInstance instance = sampleInstance();
        WorkflowTask task = new WorkflowTask(instance, instance.getWorkflowDefinition().getSteps().get(0), approver);
        WorkflowEvent event = new WorkflowEvent(WorkflowEvent.EventType.APPROVED, instance, task, approver, "Looks good");

        // Must not throw exception
        assertThatCode(() -> notificationService.onWorkflowEvent(event)).doesNotThrowAnyException();
    }

    private static WorkflowInstance sampleInstance() {
        User requester = new User("karan", "karan@example.com", "pass");
        requester.setId(1L);
        WorkflowDefinition def = new WorkflowDefinition("Leave Request", "Desc", "LEAVE_REQUEST");
        def.getSteps().add(new WorkflowStep(def, 1, "Manager Review", StepType.APPROVAL, ERole.ROLE_MANAGER, true));
        WorkflowInstance instance = new WorkflowInstance(def, requester);
        instance.setId(10L);
        return instance;
    }
}
