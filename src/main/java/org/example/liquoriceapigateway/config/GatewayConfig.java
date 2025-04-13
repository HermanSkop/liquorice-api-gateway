package org.example.liquoriceapigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Value("${app.base_path}")
    private String basePath;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("product-service", r -> r
                        .path(basePath + "/products/**")
                        .uri("lb://liquorice-product-service"))

                .route("order-service", r -> r
                        .path(basePath + "/orders/**")
                        .uri("lb://liquorice-order-service"))

                .route("cart-service", r -> r
                        .path(basePath + "/cart/**")
                        .uri("lb://liquorice-cart-service"))

                .route("auth-service", r -> r
                        .path(basePath + "/auth/**")
                        .uri("lb://liquorice-auth-service"))

                .route("payment-service", r -> r
                        .path(basePath + "/payments/**")
                        .uri("lb://liquorice-payment-service"))
                .build();
    }

}
