package org.example.liquoriceapigateway.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Collection;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String id;
    private String email;
    private String hashedPassword;
    private Collection<String> roles;
    private boolean authenticated;
    private String errorMessage;
}
