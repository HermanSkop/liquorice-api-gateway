package org.example.liquoriceapigateway.dtos.product.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class SetAvailabilityRequest {
    private String productId;
    private boolean isAvailable;
}
