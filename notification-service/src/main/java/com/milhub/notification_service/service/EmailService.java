package com.milhub.notification_service.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Async
    @Retryable(
            retryFor = {MessagingException.class, MailException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void sendEmail(String to, String subject, String htmlContent) throws MessagingException {
        log.info("Sending email to: {} with subject: {}", to, subject);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom("no-reply@milhub.com");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
        log.info("Email sent successfully to {}", to);
    }

    public void sendOrderConfirmation(String toEmail, String orderId, BigDecimal amount, String products) {
        Context context = new Context();
        context.setVariable("orderId", orderId);
        context.setVariable("products", products);
        context.setVariable("amount", amount);

        String html = templateEngine.process("order-confirmation", context);
        try {
            sendEmail(toEmail, "MilHub: Замовлення #" + orderId, html);
        } catch (Exception e) {
            log.error("Failed to queue email for order {}", orderId, e);
        }
    }

    public void sendPaymentSuccess(String toEmail, String orderId, BigDecimal amount) {
        Context context = new Context();
        context.setVariable("orderId", orderId);
        context.setVariable("amount", amount);

        String html = templateEngine.process("payment-success", context);
        try {
            sendEmail(toEmail, "MilHub: Оплата зарахована #" + orderId, html);
        } catch (Exception e) {
            log.error("Failed to queue email for payment success {}", orderId, e);
        }
    }

    public void sendDeliveryUpdate(String toEmail, String orderId, String status) {
        String statusText = translateStatus(status);
        Context context = new Context();
        context.setVariable("orderId", orderId);
        context.setVariable("statusText", statusText);

        String html = templateEngine.process("delivery-update", context);
        try {
            sendEmail(toEmail, "MilHub: Оновлення замовлення #" + orderId, html);
        } catch (Exception e) {
            log.error("Failed to queue email for delivery update {}", orderId, e);
        }
    }

    private String translateStatus(String status) {
        return switch (status) {
            case "SHIPPED" -> "Відправлено";
            case "DELIVERED" -> "Доставлено";
            case "READY_FOR_PICKUP" -> "Чекає у точці видачі";
            case "CANCELLED" -> "Скасовано";
            default -> status;
        };
    }

    public void sendVerificationApproved(String toEmail) {
        Context context = new Context();
        String html = templateEngine.process("verification-approved", context);
        try {
            sendEmail(toEmail, "MilHub: Акаунт верифіковано", html);
        } catch (Exception e) {
            log.error("Failed to queue email for verification approved", e);
        }
    }

    public void sendVerificationRejected(String toEmail, String reason) {
        Context context = new Context();
        context.setVariable("reason", reason != null ? reason : "Причина не вказана");

        String html = templateEngine.process("verification-rejected", context);
        try {
            sendEmail(toEmail, "MilHub: Відмова у верифікації", html);
        } catch (Exception e) {
            log.error("Failed to queue email for verification rejected", e);
        }
    }
}