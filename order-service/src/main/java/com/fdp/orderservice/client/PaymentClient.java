package com.fdp.orderservice.client;

import com.fdp.orderservice.dto.payment.PaymentRequest;
import com.fdp.orderservice.dto.payment.PaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "PAYMENT-SERVICE", path = "/api/payments")
public interface PaymentClient {

    @PostMapping
    PaymentResponse processPayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody PaymentRequest request
    );
}
