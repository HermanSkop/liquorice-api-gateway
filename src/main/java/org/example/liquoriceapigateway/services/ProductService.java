package org.example.liquoriceapigateway.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.liquoriceapigateway.dtos.PagedResponse;
import org.example.liquoriceapigateway.dtos.ProductDto;
import org.example.liquoriceapigateway.dtos.product.request.GetCategoriesRequest;
import org.example.liquoriceapigateway.dtos.product.request.GetProductsRequest;
import org.example.liquoriceapigateway.dtos.product.request.SetAvailabilityRequest;
import org.example.liquoriceapigateway.dtos.product.response.GetCategoriesResponse;
import org.example.liquoriceapigateway.dtos.product.response.GetProductsResponse;
import org.example.liquoriceapigateway.dtos.product.response.SetAvailabilityResponse;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
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

    public Mono<PagedResponse<ProductDto>> getProductPreviewDtos(String search, List<String> categories, int pageNumber, int pageSize, List<String> sortList) {
        log.info("Getting product previews with search='{}', categories={}, page={}, size={}",
                search, categories, pageNumber, pageSize);

        GetProductsRequest payload = GetProductsRequest.builder()
                .pageNumber(pageNumber)
                .pageSize(pageSize)
                .sort(sortList)
                .search(search)
                .categories(categories)
                .build();

        return kafkaProducerService.sendAndReceive(productTopic, payload)
                .map(response -> {
                    log.debug("Received products response: {}", response);
                    return (GetProductsResponse) response;
                })
                .map(GetProductsResponse::getProducts)
                .doOnSubscribe(s -> log.debug("Subscribed to products request response"))
                .doOnError(e -> log.error("Failed to get products", e));
    }

    public Mono<List<String>> getAllCategories() {
        log.debug("Sending GET_CATEGORIES request to topic: {}", productTopic);


        return kafkaProducerService.sendAndReceive(productTopic, new GetCategoriesRequest()).cast(GetCategoriesResponse.class)
                .doOnNext(response -> log.debug("Received categories response: {}", response))
                .doOnSuccess(response -> log.info("Received {} categories", response.getCategories().size()))
                .doOnError(e -> log.error("Failed to get product categories", e))
                .map(GetCategoriesResponse::getCategories);
    }

    public Mono<ProductDto> setAvailable(String productId, boolean isAvailable) {
        log.info("Setting product {} availability to {}", productId, isAvailable);

        SetAvailabilityRequest payload = SetAvailabilityRequest.builder()
                .productId(productId)
                .isAvailable(isAvailable)
                .build();

        log.debug("Sending SET_AVAILABLE request to topic: {}", productTopic);
        return kafkaProducerService.sendAndReceive(productTopic, payload)
                .cast(SetAvailabilityResponse.class)
                .map(SetAvailabilityResponse::getProduct)
                .doOnNext(response -> log.debug("Received updated product: {}", response));
    }
}
