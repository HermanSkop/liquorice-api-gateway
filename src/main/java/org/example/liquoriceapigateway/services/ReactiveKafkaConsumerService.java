package org.example.liquoriceapigateway.services;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Service
@Slf4j
public class ReactiveKafkaConsumerService {

    private final ReactiveKafkaProducerService producerService;
    private final String responseTopics;
    private final ReceiverOptions<String, Object> receiverOptions;
    
    private KafkaReceiver<String, Object> receiver;
    
    public ReactiveKafkaConsumerService(
            ReactiveKafkaProducerService producerService,
            @Value("${kafka.topics.user-auth-response}") String responseTopics,
            ReceiverOptions<String, Object> receiverOptions) {
        this.producerService = producerService;
        this.responseTopics = responseTopics;
        this.receiverOptions = receiverOptions;
    }
    
    @PostConstruct
    public void init() {
        // Configure subscription to response topics
        ReceiverOptions<String, Object> options = receiverOptions
                .subscription(Collections.singleton(responseTopics));

        receiver = KafkaReceiver.create(options);

        // Start consuming messages and route them to the producer service
        receiver.receive()
                .subscribe(record -> {
                    String correlationId = null;
                    if (record.headers().lastHeader("correlationId") != null) {
                        correlationId = new String(record.headers().lastHeader("correlationId").value(),
                                StandardCharsets.UTF_8);
                    } else if (record.key() != null) {
                        correlationId = record.key();
                    }

                    if (correlationId != null) {
                        log.debug("Received reactive response for correlationId: {}", correlationId);
                        producerService.completeResponse(correlationId, record.value());
                    } else {
                        log.warn("Received reactive message without correlation ID: {}", record.value());
                    }

                    record.receiverOffset().acknowledge();
                }, error -> {
                    log.error("Error in reactive Kafka consumer", error);
                });
    }
    
    @PreDestroy
    public void shutdown() {
        // Clean shutdown if needed
    }
}
