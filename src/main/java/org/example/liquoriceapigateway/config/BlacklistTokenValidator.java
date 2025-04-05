package org.example.liquoriceapigateway.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.liquoriceapigateway.services.TokenBlacklistService;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class BlacklistTokenValidator {
    
    private static final OAuth2Error BLACKLISTED_TOKEN_ERROR = new OAuth2Error(
            "token_blacklisted",
            "The token is blacklisted",
            null
    );
    
    private final TokenBlacklistService tokenBlacklistService;
    
    /**
     * Validates that a token is not blacklisted
     * 
     * @param jwt The JWT token to validate
     * @return Mono with the validation result
     */
    public Mono<OAuth2TokenValidatorResult> validate(Jwt jwt) {
        String token = jwt.getTokenValue();
        
        return tokenBlacklistService.isTokenBlacklisted(token)
                .map(isBlacklisted -> {
                    if (isBlacklisted) {
                        log.debug("Token is blacklisted: {}", token);
                        return OAuth2TokenValidatorResult.failure(BLACKLISTED_TOKEN_ERROR);
                    } else {
                        return OAuth2TokenValidatorResult.success();
                    }
                });
    }
    
    /**
     * Convert authentication to Mono, validating blacklisted tokens
     * 
     * @param authentication JwtAuthenticationToken to validate
     * @return Mono of the authentication or empty if token is blacklisted
     */
    public Mono<JwtAuthenticationToken> validateToken(JwtAuthenticationToken authentication) {
        Jwt jwt = authentication.getToken();
        String token = jwt.getTokenValue();
        
        return tokenBlacklistService.isTokenBlacklisted(token)
                .flatMap(isBlacklisted -> {
                    if (isBlacklisted) {
                        log.debug("Token is blacklisted: {}", token);
                        return Mono.empty();
                    }
                    return Mono.just(authentication);
                });
    }
}
