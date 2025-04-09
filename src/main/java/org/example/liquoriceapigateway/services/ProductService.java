package org.example.liquoriceapigateway.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.liquoriceapigateway.dtos.PagedResponse;
import org.example.liquoriceapigateway.dtos.ProductPreviewDto;
import org.example.liquoriceapigateway.dtos.product.request.GetCategoriesRequest;
import org.example.liquoriceapigateway.dtos.product.request.GetProductsRequest;
import org.example.liquoriceapigateway.dtos.product.request.SetAvailabilityRequest;
import org.example.liquoriceapigateway.dtos.product.response.GetCategoriesResponse;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

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

        GetProductsRequest payload = GetProductsRequest.builder()
                .pageable(pageable)
                .search(search)
                .categories(categories)
                .build();

        return null; /*kafkaProducerService.getProducts(productTopic, payload)
                .doOnSubscribe(s -> log.debug("Subscribed to products request response"))
                .doOnSuccess(response -> log.info("Received {} products",
                    response.getContent() != null ? response.getContent().size() : 0))
                .doOnError(e -> log.error("Failed to get products", e));*/
    }

    public Mono<List<String>> getAllCategories() {
        log.debug("Sending GET_CATEGORIES request to topic: {}", productTopic);

        GetCategoriesRequest request = new GetCategoriesRequest();

        return kafkaProducerService.getCategories(productTopic, request)
            .doOnSubscribe(s -> log.debug("Subscribed to categories request response"))
            .doOnSuccess(response -> log.info("Received {} categories",
                response.getCategories().size()))
            .doOnError(e -> log.error("Failed to get product categories", e))
            .map(GetCategoriesResponse::getCategories);
    }

    public Mono<ProductPreviewDto> setAvailable(String productId, boolean isAvailable) {
        log.info("Setting product {} availability to {}", productId, isAvailable);

        SetAvailabilityRequest payload = SetAvailabilityRequest.builder()
                .productId(productId)
                .isAvailable(isAvailable)
                .build();

        log.debug("Sending SET_AVAILABLE request to topic: {}", productTopic);
        return kafkaProducerService.setProductAvailability(productTopic, payload)
            .doOnSubscribe(s -> log.debug("Subscribed to set availability request response"))
            .doOnSuccess(response -> log.info("Successfully updated availability for product {}", productId))
            .doOnError(e -> log.error("Failed to update product availability for {}", productId, e));
    }
}
