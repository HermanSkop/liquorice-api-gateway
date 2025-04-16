package org.example.liquoriceapigateway.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        tokenBlacklistService = new TokenBlacklistService(reactiveRedisTemplate);
    }

    @Test
    void isTokenBlacklisted_whenTokenExists_shouldReturnTrue() {
        String token = "blacklisted-token";
        when(reactiveRedisTemplate.hasKey(token)).thenReturn(Mono.just(true));

        StepVerifier.create(tokenBlacklistService.isTokenBlacklisted(token))
                .expectNext(true)
                .verifyComplete();

        verify(reactiveRedisTemplate).hasKey(token);
    }

    @Test
    void isTokenBlacklisted_whenTokenDoesNotExist_shouldReturnFalse() {
        String token = "valid-token";
        when(reactiveRedisTemplate.hasKey(token)).thenReturn(Mono.just(false));

        StepVerifier.create(tokenBlacklistService.isTokenBlacklisted(token))
                .expectNext(false)
                .verifyComplete();

        verify(reactiveRedisTemplate).hasKey(token);
    }
}
