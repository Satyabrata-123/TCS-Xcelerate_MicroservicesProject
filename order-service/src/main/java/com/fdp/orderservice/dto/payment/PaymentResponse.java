package com.fdp.orderservice.dto.payment;

import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private String status;
    private String message;
    private PaymentData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentData {
        private PaymentDetail payment;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentDetail {
        private UUID id;
        private UUID orderId;
        private BigDecimal amount;
        private String currency;
        private String paymentMethod;
        private String status;
        private String idempotencyKey;
        private String errorMessage;
    }
}
