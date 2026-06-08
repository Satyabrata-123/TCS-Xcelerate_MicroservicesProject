package com.fdp.orderservice.dto.event;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDeliveredEvent {
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
        private LocalDateTime deliveredAt;
    }
}
