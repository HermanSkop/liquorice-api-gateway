package org.example.liquoriceapigateway.config;

import org.example.liquoriceapigateway.services.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlacklistTokenValidatorTest {

    @Mock
    private TokenBlacklistService tokenBlacklistService;
    
    @Mock
    private Jwt jwt;
    
    private BlacklistTokenValidator blacklistTokenValidator;
    private JwtAuthenticationToken jwtAuthenticationToken;
    
    @BeforeEach
    void setUp() {
        blacklistTokenValidator = new BlacklistTokenValidator(tokenBlacklistService);
        when(jwt.getTokenValue()).thenReturn("test-token");
        jwtAuthenticationToken = new JwtAuthenticationToken(jwt, Collections.emptyList());
    }
    
    @Test
    void validateToken_whenTokenIsNotBlacklisted_shouldReturnAuthentication() {
        when(tokenBlacklistService.isTokenBlacklisted("test-token")).thenReturn(Mono.just(false));
        
        StepVerifier.create(blacklistTokenValidator.validateToken(jwtAuthenticationToken))
                .expectNext(jwtAuthenticationToken)
                .verifyComplete();
    }
    
    @Test
    void validateToken_whenTokenIsBlacklisted_shouldThrowInvalidBearerTokenException() {
        when(tokenBlacklistService.isTokenBlacklisted("test-token")).thenReturn(Mono.just(true));
        
        StepVerifier.create(blacklistTokenValidator.validateToken(jwtAuthenticationToken))
                .expectError(InvalidBearerTokenException.class)
                .verify();
    }
}
