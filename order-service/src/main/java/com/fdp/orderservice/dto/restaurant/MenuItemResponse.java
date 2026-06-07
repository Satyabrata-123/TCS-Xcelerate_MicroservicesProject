package com.fdp.orderservice.dto.restaurant;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItemResponse {
    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;

    @JsonProperty("is_available")
    private boolean isAvailable;
}
