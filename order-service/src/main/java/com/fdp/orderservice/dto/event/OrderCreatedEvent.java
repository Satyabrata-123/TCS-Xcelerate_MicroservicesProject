package com.fdp.orderservice.dto.event;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreatedEvent {
    private UUID eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private Payload payload;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Payload {
        private UUID orderId;
        private UUID customerId;
        private String restaurantId;
        private BigDecimal totalAmount;
        private List<OrderItemDto> items;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemDto {
        private String menuItemId;
        private String name;
        private BigDecimal price;
        private Integer quantity;
    }
}
