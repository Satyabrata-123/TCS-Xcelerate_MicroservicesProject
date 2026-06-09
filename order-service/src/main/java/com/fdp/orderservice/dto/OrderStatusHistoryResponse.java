package com.fdp.orderservice.dto;

import com.fdp.orderservice.entity.OrderStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusHistoryResponse {
    private UUID id;
    private OrderStatus previousStatus;
    private OrderStatus newStatus;
    private LocalDateTime transitionTime;
    private String reason;
}
