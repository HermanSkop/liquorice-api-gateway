package org.example.liquoriceapigateway.dtos.product.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.liquoriceapigateway.dtos.PagedResponse;
import org.example.liquoriceapigateway.dtos.product.ProductDto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetProductsResponse {
    private PagedResponse<ProductDto> products;
}
