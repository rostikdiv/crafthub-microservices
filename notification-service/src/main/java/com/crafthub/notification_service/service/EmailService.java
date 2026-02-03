package com.crafthub.notification_service.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendEmail(String to, String subject, String htmlContent) {
        try {
            log.info("📨 Sending email to: {}", to);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("no-reply@crafthub.com");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("✅ Email sent successfully to {}", to);
        } catch (MessagingException e) {
            log.error("❌ Failed to send email to {}", to, e);
        }
    }

    // 1. Замовлення створено
    public void sendOrderConfirmation(String toEmail, String orderId, BigDecimal amount, String products) {
        String html = """
            <h2>Замовлення #%s створено!</h2>
            <p>Дякуємо за покупку.</p>
            <p><strong>Товари:</strong> %s</p>
            <p><strong>Сума:</strong> %s UAH</p>
            <p>Будь ласка, перейдіть до оплати, якщо ви цього ще не зробили.</p>
            """.formatted(orderId, products, amount);
        sendEmail(toEmail, "CraftHub: Замовлення #" + orderId, html);
    }

    // 2. Оплата успішна
    public void sendPaymentSuccess(String toEmail, String orderId, BigDecimal amount) {
        String html = """
            <h2 style="color: green;">Оплата успішна! ✅</h2>
            <p>Ваше замовлення #%s на суму <strong>%s UAH</strong> успішно оплачено.</p>
            <p>Ми починаємо комплектацію.</p>
            """.formatted(orderId, amount);
        sendEmail(toEmail, "CraftHub: Оплата зарахована #" + orderId, html);
    }

    // 3. Зміна статусу доставки
    public void sendDeliveryUpdate(String toEmail, String orderId, String status) {
        String statusText = translateStatus(status);
        String html = """
            <h2>Оновлення статусу доставки 🚚</h2>
            <p>Статус вашого замовлення #%s змінився на: <strong>%s</strong></p>
            """.formatted(orderId, statusText);
        sendEmail(toEmail, "CraftHub: Оновлення замовлення #" + orderId, html);
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
}