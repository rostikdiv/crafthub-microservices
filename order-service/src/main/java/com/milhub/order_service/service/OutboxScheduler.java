package com.milhub.order_service.service;

import com.milhub.order_service.entity.OutboxEvent;
import com.milhub.order_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableScheduling
public class OutboxScheduler {

    private final OutboxEventRepository outboxEventRepository;
    
    @Autowired(required = false)
    private KafkaPublisherService kafkaPublisherService;

    // Optional integration depending on the environment (e.g. SQS). 
    // Here we focus on Kafka to simplify. We can check profiles.
    
    @Scheduled(fixedDelay = 5000) // Run every 5 seconds
    @Transactional
    public void processOutboxEvents() {
        // Fetch up to 50 pending events
        List<OutboxEvent> events = outboxEventRepository.findByStatusOrderByCreatedAtAsc("PENDING", PageRequest.of(0, 50));
        
        if (events.isEmpty()) {
            return;
        }
        
        log.info("Processing {} outbox events...", events.size());
        
        for (OutboxEvent event : events) {
            try {
                // We determine the topic by event type or a convention.
                // Assuming "OrderPlacedEvent" -> "order-placed-topic"
                // Assuming "RefundApprovedEvent" -> "return-events"
                String topic = "OrderPlacedEvent".equals(event.getEventType()) ? "order-placed-topic" : "return-events";
                
                if (kafkaPublisherService != null) {
                    kafkaPublisherService.sendJsonEvent(topic, event.getAggregateId(), event.getPayload());
                } else {
                    log.debug("KafkaPublisherService is not available. Skipping publish for event {}", event.getId());
                }
                
                event.setStatus("PROCESSED");
                outboxEventRepository.save(event);
            } catch (Exception e) {
                log.error("Failed to process event {}: {}", event.getId(), e.getMessage());
                // We leave it as PENDING to retry next time
            }
        }
    }
}
