package org.example.liquoriceapigateway.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    public Mono<Boolean> isTokenBlacklisted(String token) {
        return reactiveRedisTemplate.hasKey(token);
    }
}
