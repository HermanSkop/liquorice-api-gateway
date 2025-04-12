package org.example.liquoriceapigateway.dtos.product;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "image")
public class ProductDto {
    private String id;
    private String name;
    private String description;
    private double price;
    private List<String> categories;
    private String image;
    private int amountLeft;
    private boolean isAvailable;
}