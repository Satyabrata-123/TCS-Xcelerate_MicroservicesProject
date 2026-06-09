package com.fdp.orderservice.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCancelRequest {

    @Size(max = 255, message = "Cancellation reason cannot exceed 255 characters")
    private String reason;
}
