package org.example.liquoriceapigateway.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.liquoriceapigateway.config.Constants;
import org.example.liquoriceapigateway.config.JwtConfig;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.time.Duration;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final JwtConfig jwtConfig;

    public Mono<Void> blacklistToken(String token, String reason) {
        long ttl = getTokenRemainingLifetimeMillis(token) + Constants.JWT_ACCESS_TOKEN_SECONDS_TIMEOUT_SKEW * 1000;
        return reactiveRedisTemplate.opsForValue().set(token, reason, Duration.ofMillis(ttl))
                .then();
    }

    public Mono<Boolean> isTokenBlacklisted(String token) {
        return reactiveRedisTemplate.hasKey(token);
    }

    public long getTokenRemainingLifetimeMillis(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Date expiration = claims.getExpiration();
            long remainingTime = expiration.getTime() - System.currentTimeMillis();

            return Math.max(0, remainingTime);
        } catch (Exception e) {
            return 0;
        }
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtConfig.getSecretKey().getBytes());
    }
}
