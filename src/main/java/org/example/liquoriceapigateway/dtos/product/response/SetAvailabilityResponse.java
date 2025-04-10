package org.example.liquoriceapigateway.dtos.product.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.liquoriceapigateway.dtos.ProductDto;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SetAvailabilityResponse {
    ProductDto product;
}
