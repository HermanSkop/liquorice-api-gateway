package org.example.liquoriceapigateway.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.example.liquoriceapigateway.dtos.product.response.GetCategoriesResponse;
import org.example.liquoriceapigateway.dtos.product.request.GetCategoriesRequest;
import org.example.liquoriceapigateway.dtos.ProductPreviewDto;
import org.example.liquoriceapigateway.dtos.product.request.SetAvailabilityRequest;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

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

    /**
     * Send a request to get categories and receive a typed response
     *
     * @param topic The Kafka topic to send to
     * @param request The GetCategoriesRequest
     * @return Mono that will emit the GetCategoriesResponse when received
     */
    public Mono<GetCategoriesResponse> getCategories(String topic, GetCategoriesRequest request) {
        return sendAndReceive(topic, request)
                .cast(GetCategoriesResponse.class)
                .doOnNext(response -> log.debug("Received categories response: {}", response));
    }

    /**
     * Send a request to get products and receive a paged response
     *
     * @param topic The Kafka topic to send to
     * @param request The GetProductsRequestDto
     * @return Mono that will emit a paged response of ProductPreviewDto when received
     */
    /*public Mono<PagedResponse<ProductPreviewDto>> getProducts(String topic, GetProductsRequestDto request) {
        *//*return sendAndReceive(topic, request)
                .cast(PagedResponse.class)
                .doOnNext(response -> log.debug("Received products response: {}", response));*//*
    }*/

    /**
     * Send a request to set product availability and receive the updated product
     *
     * @param topic The Kafka topic to send to
     * @param request The SetAvailabilityRequestDto
     * @return Mono that will emit the updated ProductPreviewDto when received
     */
    public Mono<ProductPreviewDto> setProductAvailability(String topic, SetAvailabilityRequest request) {
        return sendAndReceive(topic, request)
                .cast(ProductPreviewDto.class)
                .doOnNext(response -> log.debug("Received updated product: {}", response));
    }
}
