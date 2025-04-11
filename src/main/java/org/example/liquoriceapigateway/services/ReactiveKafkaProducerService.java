package org.example.liquoriceapigateway.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.example.liquoriceapigateway.dtos.ProductDto;
import org.example.liquoriceapigateway.dtos.product.request.SetAvailabilityRequest;
import org.example.liquoriceapigateway.dtos.product.response.SetAvailabilityResponse;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReactiveKafkaProducerService {
    private final ReplyingKafkaTemplate<String, Object, ?> replyingKafkaTemplate;

    /**
     * Generic method to send a message to Kafka and receive a response
     *
     * @param topic The Kafka topic to send to
     * @param data The data to send
     * @return Mono that will emit the response when received
     */
    public Mono<Object> sendAndReceive(String topic, Object data) {
        log.debug("Request data: {}", data);

        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, data);

        return Mono.fromFuture(() -> replyingKafkaTemplate.sendAndReceive(record).toCompletableFuture())
                .doOnSubscribe(s -> log.debug("Subscribed to response"))
                .doOnSuccess(response -> log.debug("Received response"))
                .doOnError(e -> log.error("Error receiving response, error: {}", e.getMessage()))
                .onErrorResume(e -> {
                    log.error("Failed to receive response for request: {}", data, e);
                    return Mono.error(e);
                })
                .map(ConsumerRecord::value);
    }
}
