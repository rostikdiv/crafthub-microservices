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

    /**
     * Відправляє лист асинхронно, щоб не блокувати Kafka Listener.
     */
    @Async
    public void sendOrderConfirmation(String toEmail, String orderId, BigDecimal amount, String products) {
        try {
            log.info("📨 Sending email to: {}", toEmail);

            MimeMessage message = mailSender.createMimeMessage();
            // true означає multipart (можливість вкладень) і підтримку UTF-8
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("no-reply@crafthub.com"); // Mailtrap дозволяє будь-якого відправника
            helper.setTo(toEmail);
            helper.setSubject("CraftHub: Замовлення #" + orderId + " створено!");

            // Формуємо красивий HTML
            String htmlContent = buildHtmlTemplate(orderId, amount, products);
            helper.setText(htmlContent, true); // true = це HTML

            mailSender.send(message);
            log.info("✅ Email sent successfully to {}", toEmail);

        } catch (MessagingException e) {
            log.error("❌ Failed to send email to {}", toEmail, e);
        }
    }

    private String buildHtmlTemplate(String orderId, BigDecimal amount, String products) {
        return """
            <div style="font-family: Helvetica, Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;">
                <h2 style="color: #2c3e50; text-align: center;">Дякуємо за покупку! 🛡️</h2>
                <p>Ваше замовлення <strong>#%s</strong> успішно прийнято.</p>
                
                <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0;">
                    <p style="margin: 5px 0;"><strong>Товари:</strong> %s</p>
                    <p style="margin: 5px 0; font-size: 18px;"><strong>Сума:</strong> <span style="color: #27ae60;">%s UAH</span></p>
                </div>

                <div style="text-align: center; margin-top: 30px;">
                    <a href="http://localhost:8080/payment/%s" style="background-color: #3498db; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; font-weight: bold;">Перейти до оплати</a>
                </div>
                
                <hr style="margin-top: 30px; border: 0; border-top: 1px solid #eee;">
                <p style="font-size: 12px; color: #999; text-align: center;">Це автоматичне повідомлення від системи CraftHub.</p>
            </div>
            """.formatted(orderId, products, amount, orderId);
        // Примітка: в останньому параметрі (посилання) ми поки що використовуємо orderId як заглушку
    }
}