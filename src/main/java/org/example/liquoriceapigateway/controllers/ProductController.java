package org.example.liquoriceapigateway.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.liquoriceapigateway.dtos.ProductDto;
import org.example.liquoriceapigateway.exceptions.NotFoundException;
import org.example.liquoriceapigateway.dtos.PagedResponse;
import org.example.liquoriceapigateway.services.ProductService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import static org.example.liquoriceapigateway.config.Constants.BASE_PATH;

@Slf4j
@RestController
@RequestMapping(BASE_PATH + "/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public Mono<PagedResponse<ProductDto>> getProducts(
            @RequestParam(required = false) int page,
            @RequestParam(required = false) int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) List<String> categories,
            @RequestParam(required = false) String sort) {

        return productService.getProductPreviewDtos(search, categories, page, size, List.of(sort.split(",")));
    }

    @GetMapping("/categories")
    public Mono<List<String>> getCategories() {
        return productService.getAllCategories();
    }

    @PatchMapping("/{productId}/available")
    public Mono<ResponseEntity<ProductDto>> setAvailable(
            @RequestBody boolean isAvailable,
            @PathVariable String productId) {
        return productService.setAvailable(productId, isAvailable)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.error(new NotFoundException("Product not found")));
    }
}
