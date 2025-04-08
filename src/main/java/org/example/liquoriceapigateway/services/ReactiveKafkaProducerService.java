package org.example.liquoriceapigateway.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReactiveKafkaProducerService {
    
    private final KafkaSender<String, Object> kafkaSender;
    private final ConcurrentHashMap<String, MonoSink<Object>> pendingRequests = new ConcurrentHashMap<>();
    

    /**
     * Sends a message to Kafka and returns a Mono that will complete when the response is received
     *
     * @param topic The Kafka topic to send to
     * @param data The data to send
     * @return Mono that will emit the response when received
     */
    public Mono<Object> sendAndReceive(String topic, Object data) {
        String correlationId = UUID.randomUUID().toString();
        
        // Create a Mono that will be completed when the response arrives
        Mono<Object> responseMono = Mono.create(sink -> pendingRequests.put(correlationId, sink));
        
        // Create a producer record with the correlation ID in the headers
        ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(topic, data);

        // Send the message
        return kafkaSender.send(Mono.just(SenderRecord.create(producerRecord, correlationId)))
                .next()
                .then(responseMono)
                .doOnError(e -> {
                    pendingRequests.remove(correlationId);
                    log.error("Error sending message to Kafka", e);
                });
    }
    
    /**
     * Called by a Kafka consumer when a response is received
     *
     * @param correlationId The correlation ID from the response
     * @param response The response data
     */
    public void completeResponse(String correlationId, Object response) {
        MonoSink<Object> pendingRequest = pendingRequests.remove(correlationId);
        if (pendingRequest != null) {
            pendingRequest.success(response);
        } else {
            log.warn("Received response for unknown correlation ID: {}", correlationId);
        }
    }
}
