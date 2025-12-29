package com.crafthub.notification_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {
    public void sendOrderConfirmation(String toEmail, String productName, String orderId) {
        log.info("📧 [EMAIL SENT] To: {}, Subject: Order #{} Confirmed, Item: {}", toEmail, orderId, productName);
    }
}