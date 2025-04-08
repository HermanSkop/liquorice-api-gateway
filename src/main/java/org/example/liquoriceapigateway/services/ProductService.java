package org.example.liquoriceapigateway.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.liquoriceapigateway.dtos.PagedResponse;
import org.example.liquoriceapigateway.dtos.ProductPreviewDto;
import org.example.liquoriceapigateway.dtos.product.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ReactiveKafkaProducerService kafkaProducerService;
    private final ModelMapper modelMapper;

    @Value("${kafka.topics.liquorice-product}")
    private String productTopic;

    public Mono<PagedResponse<ProductPreviewDto>> getProductPreviewDtos(Pageable pageable, String search, List<String> categories) {
        log.info("Getting product previews with search='{}', categories={}, page={}, size={}",
                search, categories, pageable.getPageNumber(), pageable.getPageSize());

        GetProductsRequestDto payload = GetProductsRequestDto.builder()
                .pageable(pageable)
                .search(search)
                .categories(categories)
                .build();

        return kafkaProducerService.sendAndReceive(productTopic, RequestType.GET_PRODUCTS, payload)
                .map(response -> new PagedResponse<>()); // TODO implement
    }

    public Mono<List<String>> getAllCategories() {
        log.debug("Sending GET_CATEGORIES request to topic: {}", productTopic);
        return kafkaProducerService.sendAndReceive(productTopic, RequestType.GET_CATEGORIES, null)
            .doOnSubscribe(s -> log.debug("Subscribed to categories request response"))
            .doOnSuccess(response -> log.info("Received {} categories",
                ((GetCategoriesResponse) response).getCategories().size()))
            .doOnError(e -> log.error("Failed to get product categories", e))
            .map(response -> ((GetCategoriesResponse) response).getCategories());
    }

    public Mono<ProductPreviewDto> setAvailable(String productId, boolean isAvailable) {
        log.info("Setting product {} availability to {}", productId, isAvailable);

        SetAvailabilityRequestDto payload = SetAvailabilityRequestDto.builder()
                .productId(productId)
                .isAvailable(isAvailable)
                .build();

        log.debug("Sending SET_AVAILABLE request to topic: {}", productTopic);
        return kafkaProducerService.sendAndReceive(productTopic, RequestType.SET_AVAILABLE, payload)
            .doOnSubscribe(s -> log.debug("Subscribed to set availability request response"))
            .doOnSuccess(response -> log.info("Successfully updated availability for product {}", productId))
            .doOnError(e -> log.error("Failed to update product availability for {}", productId, e))
            .cast(ProductPreviewDto.class);
    }
}
