package com.fdp.orderservice.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {
    private Long id;
    private String menuItemId;
    private String name;
    private BigDecimal price;
    private Integer quantity;
}
