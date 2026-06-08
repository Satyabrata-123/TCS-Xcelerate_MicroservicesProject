package com.fdp.orderservice.dto.event;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSuccessEvent {
    private UUID eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private Payload payload;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Payload {
        private UUID paymentId;
        private UUID orderId;
        private BigDecimal amount;
        private String currency;
        private String paymentMethod;
        private String idempotencyKey;
    }
}
