package org.example.liquoriceapigateway.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.example.liquoriceapigateway.dtos.product.RequestType;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReactiveKafkaProducerService {
    
    private final KafkaSender<String, Object> kafkaSender;
    private final ConcurrentHashMap<String, MonoSink<Object>> pendingRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant> requestTimestamps = new ConcurrentHashMap<>();

    private final AtomicLong sentCount = new AtomicLong(0);
    private final AtomicLong completedCount = new AtomicLong(0);
    private final AtomicLong timeoutCount = new AtomicLong(0);

    /**
     * Sends a message to Kafka and returns a Mono that will complete when the response is received
     *
     * @param topic The Kafka topic to send to
     * @param data The data to send
     * @return Mono that will emit the response when received
     */
    public Mono<Object> sendAndReceive(String topic, RequestType type, Object data) {
        String correlationId = UUID.randomUUID().toString();
        long currentCount = sentCount.incrementAndGet();

        log.info("Sending request #{} with correlationId {} to topic: {}",
                currentCount, correlationId, topic);
        log.debug("Request data: {}", data);

        Mono<Object> responseMono = Mono.create(sink -> pendingRequests.put(correlationId, sink));
        
        ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(topic, correlationId, data);

        producerRecord.headers().add(new RecordHeader(KafkaHeaders.CORRELATION_ID, correlationId.getBytes(StandardCharsets.UTF_8)));
        producerRecord.headers().add(new RecordHeader("messageType", type.name().getBytes(StandardCharsets.UTF_8)));

        log.info("Sending request with correlationId {} to {}", correlationId, topic);

        return kafkaSender.send(Mono.just(SenderRecord.create(producerRecord, correlationId)))
                .next()
                .then(responseMono)
                .doOnError(e -> {
                    pendingRequests.remove(correlationId);
                    requestTimestamps.remove(correlationId);
                    if (e instanceof java.util.concurrent.TimeoutException) {
                        timeoutCount.incrementAndGet();
                        log.warn("Timeout waiting for response to request #{} (correlationId: {})",
                                currentCount, correlationId);
                    } else {
                        log.error("Error for request #{} (correlationId: {}) to topic: {}",
                                currentCount, correlationId, topic, e);
                    }
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
            log.debug("Completed response for correlationId: {}", correlationId);
        } else {
            log.warn("Received response for unknown correlation ID: {}", correlationId);
        }
    }

    @Scheduled(fixedRate = 60000)
    public void reportStats() {
        int pendingCount = pendingRequests.size();
        log.info("Kafka messaging stats - Sent: {}, Completed: {}, Timeouts: {}, Pending: {}",
                sentCount.get(), completedCount.get(), timeoutCount.get(), pendingCount);

        // Check for long-running pending requests
        if (pendingCount > 0) {
            Instant now = Instant.now();
            requestTimestamps.forEach((correlationId, timestamp) -> {
                Duration pendingDuration = Duration.between(timestamp, now);
                if (pendingDuration.toSeconds() > 30) {
                    log.warn("Request with correlationId {} has been pending for {} seconds",
                            correlationId, pendingDuration.toSeconds());
                }
            });
        }
    }
}
