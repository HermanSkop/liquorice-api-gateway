package org.example.liquoriceapigateway.dtos.product.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
public class GetProductsRequest {
    private int pageNumber;
    private int pageSize;
    private List<String> sort;
    private String search;
    private List<String> categories;
}
