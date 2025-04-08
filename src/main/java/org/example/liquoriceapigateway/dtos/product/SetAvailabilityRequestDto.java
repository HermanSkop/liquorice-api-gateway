package org.example.liquoriceapigateway.dtos.product;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class SetAvailabilityRequestDto {
    private String productId;
    private boolean isAvailable;
}
