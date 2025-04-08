package org.example.liquoriceapigateway.services;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.Header;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverRecord;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReactiveKafkaConsumerService {

    private final ReactiveKafkaProducerService producerService;
    private final ReceiverOptions<String, Object> receiverOptions;

    @Value("${kafka.topics.liquorice-product}")
    private String productTopic;

    private Disposable subscription;
    private final AtomicLong messageCount = new AtomicLong(0);
    private final AtomicLong responseCount = new AtomicLong(0);

    @PostConstruct
    public void init() {
        log.info("Initializing Reactive Kafka Consumer Service");
        List<String> topics = List.of(productTopic);
        log.info("Subscribing to topics: {}", topics);
        subscribe(topics);
    }

    private void subscribe(List<String> topics) {
        if (subscription != null && !subscription.isDisposed()) {
            log.info("Disposing existing subscription before reconfiguring");
            subscription.dispose();
        }

        log.info("Creating new Kafka receiver subscription for topics: {}", topics);
        ReceiverOptions<String, Object> options = receiverOptions.subscription(topics);
        KafkaReceiver<String, Object> receiver = KafkaReceiver.create(options);

        subscription = receiver.receive()
                .doOnNext(record -> {
                    long count = messageCount.incrementAndGet();
                    if (count % 100 == 0) {
                        log.info("Processed {} Kafka messages total", count);
                    }
                    log.debug("Received message from topic: {}, partition: {}, offset: {}, key: {}",
                            record.topic(), record.partition(), record.offset(), record.key());
                })
                .subscribe(this::processRecord, error -> {
                    log.error("Fatal error in Kafka consumer subscription", error);
                    // Consider implementing reconnection logic here
                });

        log.info("Kafka consumer subscription established");
    }

    private void processRecord(ReceiverRecord<String, Object> record) {
        try {
            String correlationId = null;
            boolean isResponse = false;

            Header correlationHeader = record.headers().lastHeader("correlationId");
            if (correlationHeader != null) {
                correlationId = new String(correlationHeader.value(), StandardCharsets.UTF_8);
                log.debug("Message has correlationId: {}", correlationId);
            }

            Header messageTypeHeader = record.headers().lastHeader("messageType");
            if (messageTypeHeader != null) {
                String messageType = new String(messageTypeHeader.value(), StandardCharsets.UTF_8);
                isResponse = "RESPONSE".equalsIgnoreCase(messageType);
                log.debug("Message type: {}", messageType);
            }

            if (correlationId == null && record.key() != null) {
                correlationId = record.key();
                log.debug("Using record key as correlationId: {}", correlationId);
            }

            if (correlationId != null) {
                if (isResponse) {
                    long responses = responseCount.incrementAndGet();
                    log.debug("Received response #{} for correlationId: {} from topic: {}",
                            responses, correlationId, record.topic());
                    producerService.completeResponse(correlationId, record.value());
                    record.receiverOffset().acknowledge();
                    log.debug("Acknowledged offset: {} for partition: {}", record.offset(), record.partition());
                } else {
                    log.debug("Received non-response message with correlationId: {} from topic: {}",
                            correlationId, record.topic());
                }
            } else {
                log.warn("Skipping message without correlation ID from topic {}, partition {}, offset {}",
                        record.topic(), record.partition(), record.offset());
            }
        } catch (Exception e) {
            log.error("Error processing Kafka message from topic: {}", record.topic(), e);
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down ReactiveKafkaConsumerService. Processed {} messages, {} responses",
                messageCount.get(), responseCount.get());
        if (subscription != null && !subscription.isDisposed()) {
            try {
                log.info("Disposing Kafka receiver subscription");
                subscription.dispose();
                log.info("Kafka receiver subscription disposed");
            } catch (Exception e) {
                log.warn("Error while disposing Kafka receiver subscription", e);
            }
        }
    }
}
