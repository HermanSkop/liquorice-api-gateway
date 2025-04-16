package org.example.liquoriceapigateway.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class SecurityConfigTest {

    @Mock
    private BlacklistTokenValidator blacklistTokenValidator;
    
    @Mock
    private ReactiveJwtDecoder jwtDecoder;
    
    @InjectMocks
    private SecurityConfig securityConfig;
    
    @Test
    void reactiveJwtDecoder_shouldCreateDecoder() {
        ReflectionTestUtils.setField(securityConfig, "jwtSecret", "ThisIsATestSecretKeyWithAtLeast256BitsForHMACSHA256Algorithm");
        
        ReactiveJwtDecoder decoder = securityConfig.reactiveJwtDecoder();
        
        assertNotNull(decoder);
    }

    @Test
    void authenticationManager_shouldValidateNonBlacklistedToken() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .claim("sub", "user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        BearerTokenAuthenticationToken bearerToken = new BearerTokenAuthenticationToken("test-token");

        when(jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        when(blacklistTokenValidator.validateToken(any(JwtAuthenticationToken.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        ReactiveAuthenticationManager manager = securityConfig.authenticationManager(jwtDecoder);

        StepVerifier.create(manager.authenticate(bearerToken))
                .expectNextMatches(auth -> auth instanceof JwtAuthenticationToken)
                .verifyComplete();
    }
}
