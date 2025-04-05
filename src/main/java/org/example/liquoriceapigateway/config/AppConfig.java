package org.example.liquoriceapigateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class AppConfig {
    @Value("${security.jwt.secret-key}")
    private String secretKey;
}
