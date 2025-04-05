package org.example.liquoriceapigateway.config;

import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtReactiveAuthenticationManager;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.List;

import static org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtConfig jwtConfig;
    private final BlacklistTokenValidator blacklistTokenValidator;
    private final JwtRoleConverter jwtRoleConverter;
    private final EmailPasswordAuthenticationProvider emailPasswordAuthenticationProvider;

    @Bean
    public SecurityWebFilterChain securityFilterChainAuth(ServerHttpSecurity http) {
        return http
                .securityMatcher(pathMatchers(Constants.BASE_PATH + "/auth/**"))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(auth -> auth.anyExchange().permitAll())
                .build();
    }

    @Bean
    public SecurityWebFilterChain securityFilterChainMain(ServerHttpSecurity http, ReactiveAuthenticationManager jwtAuthenticationManager) {
        return http
                .securityMatcher(pathMatchers(Constants.BASE_PATH))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
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

    /**
     * Primary authentication manager that delegates to appropriate authentication manager based on the authentication type.
     * This resolves the "expected single matching bean but found 2" error.
     */
    @Bean
    @Primary
    public ReactiveAuthenticationManager primaryAuthenticationManager(ReactiveAuthenticationManager jwtAuthenticationManager) {
        return authentication -> {
            // If it's a JWT token, use the JWT authentication manager
            if (authentication instanceof org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken) {
                return jwtAuthenticationManager.authenticate(authentication);
            }

            // Otherwise use the email/password authentication manager
            return emailPasswordAuthenticationProvider.authenticate(authentication);
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        SecretKey signingKey = Keys.hmacShaKeyFor(jwtConfig.getSecretKey().getBytes());
        return NimbusReactiveJwtDecoder.withSecretKey(signingKey).build();
    }
}
