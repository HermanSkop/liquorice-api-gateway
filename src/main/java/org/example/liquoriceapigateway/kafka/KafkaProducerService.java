package org.example.liquoriceapigateway.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ConcurrentMap<String, CompletableFuture<Object>> pendingRequests = new ConcurrentHashMap<>();

    /**
     * Send a message to a specified topic and wait for a response
     * 
     * @param topic The Kafka topic to send the message to
     * @param message The message payload
     * @return CompletableFuture that will be completed when a response is received
     */
    public CompletableFuture<Object> sendAndReceive(String topic, Object message) {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<Object> responsePromise = new CompletableFuture<>();
        
        pendingRequests.put(correlationId, responsePromise);
        
        kafkaTemplate.send(topic, correlationId, message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send message to topic {}: {}", topic, ex.getMessage());
                        pendingRequests.remove(correlationId);
                        responsePromise.completeExceptionally(ex);
                    } else {
                        log.info("Message sent to topic {} with correlation ID {}", topic, correlationId);
                    }
                });
                
        return responsePromise;
    }
    
    /**
     * Send a message to a topic without waiting for a response
     * 
     * @param topic The Kafka topic to send the message to
     * @param message The message payload
     * @return CompletableFuture with the send result
     */
    public CompletableFuture<SendResult<String, Object>> send(String topic, Object message) {
        return kafkaTemplate.send(topic, message);
    }
    
    /**
     * Completes a pending request with the received response
     * 
     * @param correlationId The correlation ID to identify the request
     * @param response The response message
     */
    public void completeRequest(String correlationId, Object response) {
        CompletableFuture<Object> request = pendingRequests.remove(correlationId);
        if (request != null) {
            request.complete(response);
        }
    }
}
