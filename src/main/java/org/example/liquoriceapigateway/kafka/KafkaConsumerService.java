package org.example.liquoriceapigateway.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final KafkaProducerService producerService;

    /**
     * Listen for responses from other services and complete the corresponding waiting request
     * 
     * @param record The Kafka consumer record
     * @param correlationId The correlation ID to match the request with its response
     * @param ack Acknowledgment to manually acknowledge message processing
     */
    @KafkaListener(topics = "${kafka.topics.response}", groupId = "${spring.kafka.consumer.group-id}")
    public void listenForResponses(ConsumerRecord<String, Object> record,
                                  @Header(value = "correlationId", required = false) String correlationId,
                                  Acknowledgment ack) {
        log.info("Received response for correlationId: {}", correlationId);
        
        try {
            if (correlationId != null) {
                producerService.completeRequest(correlationId, record.value());
                log.debug("Completed request for correlationId: {}", correlationId);
            } else {
                log.warn("Received message without correlation ID: {}", record.value());
            }
            // Acknowledge successful processing
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing response for correlationId: {}", correlationId, e);
            // Do not acknowledge - message will be redelivered
        }
    }
    
    /**
     * Example listener for notifications (not expecting responses)
     * 
     * @param message The notification message
     * @param ack Acknowledgment to manually acknowledge message processing
     */
    @KafkaListener(topics = "${kafka.topics.notifications}", groupId = "${spring.kafka.consumer.group-id}")
    public void listenForNotifications(Object message, Acknowledgment ack) {
        log.info("Received notification: {}", message);
        try {
            // Process notification
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing notification: {}", message, e);
            // Do not acknowledge - message will be redelivered
        }
    }
}
