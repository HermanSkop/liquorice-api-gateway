package org.example.liquoriceapigateway.config;

import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtReactiveAuthenticationManager;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;

import java.util.List;

import static org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final BlacklistTokenValidator blacklistTokenValidator;
    private final JwtRoleConverter jwtRoleConverter;
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Bean
    public SecurityWebFilterChain securityFilterChainMain(ServerHttpSecurity http, ReactiveAuthenticationManager jwtAuthenticationManager) {
        return http
                .securityMatcher(pathMatchers(Constants.BASE_PATH + "/**"))
                .cors(cors -> cors.configurationSource(reactiveCorsConfigurationSource()))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(auth -> auth
                        .pathMatchers(HttpMethod.PATCH,
                                Constants.BASE_PATH + "/products/{productId}/available",
                                Constants.BASE_PATH + "/orders/{orderId}/refund",
                                Constants.BASE_PATH + "/customers/{customerId}/orders")
                        .hasRole("ADMIN")
                        .pathMatchers(Constants.BASE_PATH + "/cart/**").hasRole("CUSTOMER")
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(grantedAuthoritiesExtractor())
                                .authenticationManager(jwtAuthenticationManager)
                        )
                )
                .build();
    }

    @Bean
    public Converter<Jwt, Mono<org.springframework.security.authentication.AbstractAuthenticationToken>> grantedAuthoritiesExtractor() {
        return new ReactiveJwtAuthenticationConverterAdapter(jwtRoleConverter);
    }

    @Bean
    public ReactiveAuthenticationManager jwtAuthenticationManager(ReactiveJwtDecoder jwtDecoder) {
        JwtReactiveAuthenticationManager authManager = new JwtReactiveAuthenticationManager(jwtDecoder);
        return authentication -> authManager.authenticate(authentication)
                .filter(auth -> auth.isAuthenticated() && auth instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken)
                .cast(org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken.class)
                .flatMap(blacklistTokenValidator::validateToken);
    }

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        SecretKey signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        return NimbusReactiveJwtDecoder.withSecretKey(signingKey).build();
    }


    private CorsConfigurationSource reactiveCorsConfigurationSource() {
        return request -> {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowCredentials(true);
            config.setAllowedOrigins(List.of(Constants.CLIENT_SERVER));
            config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
            config.setAllowedHeaders(List.of("*"));
            config.setMaxAge(3600L);
            return config;
        };
    }
}
