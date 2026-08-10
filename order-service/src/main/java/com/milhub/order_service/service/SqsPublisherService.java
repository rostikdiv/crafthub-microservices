package com.milhub.order_service.service;

import com.milhub.order_service.dto.event.OrderPlacedEventDTO;
import io.awspring.cloud.sqs.operations.SqsTemplate; // Залежність spring-cloud-aws-starter-sqs
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("aws")
public class SqsPublisherService {

    private final SqsTemplate sqsTemplate;

    // URL черги береться з application.yaml
    @Value("${application.sqs.queue-url:order-queue}")
    private String queueUrl;

    public void sendOrderToQueue(OrderPlacedEventDTO event) {
        log.info("📤 Sending order event to SQS queue '{}': {}", queueUrl, event.orderId());

        try {
            sqsTemplate.send(to -> to
                    .queue(queueUrl)
                    .payload(event)
            );
            log.info("✅ SQS message sent");
        } catch (Exception e) {
            log.error("❌ Failed to send SQS message: {}", e.getMessage());
        }
    }
}