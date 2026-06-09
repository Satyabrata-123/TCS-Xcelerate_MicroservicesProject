package com.fdp.orderservice.dto.payment;

import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {
    private UUID orderId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
}
