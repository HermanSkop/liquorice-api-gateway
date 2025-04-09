package org.example.liquoriceapigateway.dtos.product.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
public class GetProductsRequest {
    private Pageable pageable;
    private String search;
    private List<String> categories;
}
