package org.example.liquoriceapigateway.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.liquoriceapigateway.kafka.ReactiveKafkaProducerService;
import org.example.liquoriceapigateway.models.AuthRequest;
import org.example.liquoriceapigateway.models.AuthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaUserService {

    private final ReactiveKafkaProducerService kafkaProducerService;

    @Value("${kafka.topics.user-auth-requests}")
    private String userAuthRequestTopic;

    @Value("${kafka.request.timeout}")
    private long requestTimeout;

    public Mono<AuthResponse> authenticateUser(String email, String password) {
        AuthRequest authRequest = new AuthRequest(email, password);
        
        log.info("Sending authentication request for user: {}", email);
        
        return kafkaProducerService.sendAndReceive(userAuthRequestTopic, authRequest)
                .timeout(Duration.ofMillis(requestTimeout))
                .map(response -> {
                    if (response instanceof AuthResponse) {
                        return (AuthResponse) response;
                    } else {
                        log.error("Received unexpected response type: {}", response.getClass().getName());
                        return createErrorResponse("Unexpected response format");
                    }
                })
                .onErrorResume(e -> {
                    if (e instanceof java.util.concurrent.TimeoutException) {
                        log.error("Authentication request timed out for user: {}", email, e);
                        return Mono.just(createErrorResponse("Authentication service timed out"));
                    } else {
                        log.error("Error executing authentication request for user: {}", email, e);
                        return Mono.just(createErrorResponse("Authentication service error"));
                    }
                });
    }
    
    private AuthResponse createErrorResponse(String errorMessage) {
        AuthResponse response = new AuthResponse();
        response.setAuthenticated(false);
        response.setErrorMessage(errorMessage);
        return response;
    }
}
