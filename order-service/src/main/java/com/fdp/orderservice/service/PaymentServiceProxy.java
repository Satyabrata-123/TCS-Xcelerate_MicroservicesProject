package com.fdp.orderservice.service;

import com.fdp.orderservice.client.PaymentClient;
import com.fdp.orderservice.dto.payment.PaymentRequest;
import com.fdp.orderservice.dto.payment.PaymentResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceProxy {

    private final PaymentClient paymentClient;

    @CircuitBreaker(name = "paymentService", fallbackMethod = "processPaymentFallback")
    @Retry(name = "paymentService")
    public PaymentResponse processPayment(String idempotencyKey, PaymentRequest request) {
        log.info("Executing resilient payment service call for Order ID: {}", request.getOrderId());
        return paymentClient.processPayment(idempotencyKey, request);
    }

    public PaymentResponse processPaymentFallback(String idempotencyKey, PaymentRequest request, Throwable t) {
        log.error("Resilience4j fallback triggered for Order ID: {} due to: {}", request.getOrderId(), t.getMessage(), t);

        return PaymentResponse.builder()
                .status("FAILED")
                .message("Payment service unavailable (Fallback active)")
                .data(PaymentResponse.PaymentData.builder()
                        .payment(PaymentResponse.PaymentDetail.builder()
                                .orderId(request.getOrderId())
                                .amount(request.getAmount())
                                .currency(request.getCurrency())
                                .paymentMethod(request.getPaymentMethod())
                                .status("FAILED")
                                .idempotencyKey(idempotencyKey)
                                .errorMessage("Payment service is currently unavailable. Fallback: " + t.getMessage())
                                .build())
                        .build())
                .build();
    }
}
