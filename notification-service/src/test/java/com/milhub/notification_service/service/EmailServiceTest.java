package com.milhub.notification_service.service;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @InjectMocks
    private EmailService emailService;

    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        Session session = Session.getInstance(new Properties());
        mimeMessage = new MimeMessage(session);
        lenient().when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    @DisplayName("sendEmail should send mime message with correct details")
    void sendEmail_Success() throws Exception {
        emailService.sendEmail("recipient@example.com", "Test Subject", "<p>Hello</p>");

        verify(mailSender).send(mimeMessage);
        assertEquals("Test Subject", mimeMessage.getSubject());
    }

    @Test
    @DisplayName("sendEmail should propagate MailSendException")
    void sendEmail_Failure() {
        doThrow(new MailSendException("SMTP error")).when(mailSender).send(any(MimeMessage.class));

        assertThrows(MailSendException.class, () ->
                emailService.sendEmail("fail@example.com", "Test", "Content"));
    }

    @Test
    @DisplayName("sendOrderConfirmation should render template and send email")
    void sendOrderConfirmation_Success() throws Exception {
        when(templateEngine.process(eq("order-confirmation"), any(Context.class)))
                .thenReturn("<html>Order Confirmed</html>");

        emailService.sendOrderConfirmation("buyer@example.com", "12345", BigDecimal.valueOf(500), "Tactical Helmet");

        verify(templateEngine).process(eq("order-confirmation"), any(Context.class));
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendOrderConfirmation should handle and log template engine exceptions")
    void sendOrderConfirmation_ExceptionHandled() {
        when(templateEngine.process(eq("order-confirmation"), any(Context.class)))
                .thenThrow(new RuntimeException("Template processing failed"));

        assertDoesNotThrow(() ->
                emailService.sendOrderConfirmation("buyer@example.com", "12345", BigDecimal.valueOf(500), "Tactical Helmet"));

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendPaymentSuccess should render template and send email")
    void sendPaymentSuccess_Success() throws Exception {
        when(templateEngine.process(eq("payment-success"), any(Context.class)))
                .thenReturn("<html>Payment Success</html>");

        emailService.sendPaymentSuccess("payer@example.com", "98765", BigDecimal.valueOf(1500));

        verify(templateEngine).process(eq("payment-success"), any(Context.class));
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendPaymentSuccess should handle and log exceptions")
    void sendPaymentSuccess_ExceptionHandled() {
        when(templateEngine.process(eq("payment-success"), any(Context.class)))
                .thenThrow(new RuntimeException("Template not found"));

        assertDoesNotThrow(() ->
                emailService.sendPaymentSuccess("payer@example.com", "98765", BigDecimal.valueOf(1500)));

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendDeliveryUpdate should handle all translateStatus cases")
    void sendDeliveryUpdate_StatusTranslations() {
        when(templateEngine.process(eq("delivery-update"), any(Context.class)))
                .thenReturn("<html>Delivery Update</html>");

        // Case: SHIPPED
        emailService.sendDeliveryUpdate("client@example.com", "ord-1", "SHIPPED");

        // Case: DELIVERED
        emailService.sendDeliveryUpdate("client@example.com", "ord-2", "DELIVERED");

        // Case: READY_FOR_PICKUP
        emailService.sendDeliveryUpdate("client@example.com", "ord-3", "READY_FOR_PICKUP");

        // Case: CANCELLED
        emailService.sendDeliveryUpdate("client@example.com", "ord-4", "CANCELLED");

        // Default case: CUSTOM_STATUS
        emailService.sendDeliveryUpdate("client@example.com", "ord-5", "CUSTOM_STATUS");

        verify(mailSender, times(5)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendDeliveryUpdate should handle and log exceptions")
    void sendDeliveryUpdate_ExceptionHandled() {
        when(templateEngine.process(eq("delivery-update"), any(Context.class)))
                .thenThrow(new RuntimeException("Thymeleaf exception"));

        assertDoesNotThrow(() ->
                emailService.sendDeliveryUpdate("client@example.com", "ord-1", "SHIPPED"));

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendVerificationApproved should render template and send email")
    void sendVerificationApproved_Success() {
        when(templateEngine.process(eq("verification-approved"), any(Context.class)))
                .thenReturn("<html>Approved</html>");

        emailService.sendVerificationApproved("seller@milhub.com");

        verify(templateEngine).process(eq("verification-approved"), any(Context.class));
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendVerificationApproved should handle and log exceptions")
    void sendVerificationApproved_ExceptionHandled() {
        when(templateEngine.process(eq("verification-approved"), any(Context.class)))
                .thenThrow(new RuntimeException("Mail server error"));

        assertDoesNotThrow(() ->
                emailService.sendVerificationApproved("seller@milhub.com"));

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendVerificationRejected should set provided reason when not null")
    void sendVerificationRejected_WithReason() {
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        when(templateEngine.process(eq("verification-rejected"), contextCaptor.capture()))
                .thenReturn("<html>Rejected</html>");

        emailService.sendVerificationRejected("seller@milhub.com", "Invalid documents");

        verify(mailSender).send(mimeMessage);
        assertEquals("Invalid documents", contextCaptor.getValue().getVariable("reason"));
    }

    @Test
    @DisplayName("sendVerificationRejected should set default reason when reason is null")
    void sendVerificationRejected_WithNullReason() {
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        when(templateEngine.process(eq("verification-rejected"), contextCaptor.capture()))
                .thenReturn("<html>Rejected</html>");

        emailService.sendVerificationRejected("seller@milhub.com", null);

        verify(mailSender).send(mimeMessage);
        assertEquals("No reason provided", contextCaptor.getValue().getVariable("reason"));
    }

    @Test
    @DisplayName("sendVerificationRejected should handle and log exceptions")
    void sendVerificationRejected_ExceptionHandled() {
        when(templateEngine.process(eq("verification-rejected"), any(Context.class)))
                .thenThrow(new RuntimeException("Rendering error"));

        assertDoesNotThrow(() ->
                emailService.sendVerificationRejected("seller@milhub.com", "Any reason"));

        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}
