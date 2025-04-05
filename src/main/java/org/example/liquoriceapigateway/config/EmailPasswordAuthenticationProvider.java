package org.example.liquoriceapigateway.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.liquoriceapigateway.models.AuthResponse;
import org.example.liquoriceapigateway.services.KafkaUserService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailPasswordAuthenticationProvider implements ReactiveAuthenticationManager {
    private final KafkaUserService kafkaUserService;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String email = authentication.getName();
        String password = authentication.getCredentials().toString();

        log.debug("Authenticating user with email: {}", email);
        
        return kafkaUserService.authenticateUser(email, password)
            .switchIfEmpty(Mono.error(new BadCredentialsException("Authentication failed")))
            .flatMap(response -> {
                if (!response.isAuthenticated()) {
                    String errorMessage = response.getErrorMessage() != null ? 
                            response.getErrorMessage() : "Invalid credentials";
                    return Mono.error(new BadCredentialsException(errorMessage));
                }
                
                if (response.getEmail() == null) {
                    return Mono.error(new UsernameNotFoundException("User not found"));
                }
                
                // Create Spring Security User from response
                User user = new User(
                        response.getEmail(),
                        response.getHashedPassword(),
                        response.getRoles().stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList())
                );
                
                return Mono.just(new UsernamePasswordAuthenticationToken(
                        user, null, user.getAuthorities()));
            });
    }
}
