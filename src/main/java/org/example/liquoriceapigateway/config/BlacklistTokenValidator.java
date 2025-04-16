package org.example.liquoriceapigateway.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.liquoriceapigateway.services.TokenBlacklistService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class BlacklistTokenValidator {

    private final TokenBlacklistService tokenBlacklistService;

    public Mono<Authentication> validateToken(JwtAuthenticationToken jwtAuth) {
        String token = jwtAuth.getToken().getTokenValue();

        return tokenBlacklistService.isTokenBlacklisted(token)
                .flatMap(isBlacklisted -> {
                    if (isBlacklisted) {
                        log.debug("Token is blacklisted: {}", token);
                        return Mono.error(new InvalidBearerTokenException("Token is blacklisted"));
                    }
                    return Mono.just(jwtAuth);
                });
    }
}
