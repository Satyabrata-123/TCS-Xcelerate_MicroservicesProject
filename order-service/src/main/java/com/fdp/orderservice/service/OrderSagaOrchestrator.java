package com.fdp.orderservice.service;

import com.fdp.orderservice.client.PaymentClient;
import com.fdp.orderservice.dto.event.PaymentFailedEvent;
import com.fdp.orderservice.dto.event.PaymentSuccessEvent;
import com.fdp.orderservice.dto.payment.PaymentRequest;
import com.fdp.orderservice.dto.payment.PaymentResponse;
import com.fdp.orderservice.entity.Order;
import com.fdp.orderservice.entity.OrderStatus;
import com.fdp.orderservice.entity.OrderStatusHistory;
import com.fdp.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderSagaOrchestrator {

    private final PaymentClient paymentClient;
    private final OrderRepository orderRepository;
    private final OrderEventProducer eventProducer;

    @Transactional
    public Order processOrderSaga(Order order) {
        log.info("Starting Saga Orchestration for Order ID: {}", order.getId());

        String idempotencyKey = "payment-saga-" + order.getId();
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .orderId(order.getId())
                .amount(order.getTotalAmount())
                .currency("USD")
                .paymentMethod("CREDIT_CARD")
                .build();

        try {
            log.info("Saga Orchestrator calling Payment Service for Order: {}", order.getId());
            PaymentResponse paymentResponse = paymentClient.processPayment(idempotencyKey, paymentRequest);

            if (paymentResponse != null && paymentResponse.getData() != null && paymentResponse.getData().getPayment() != null
                    && "SUCCESS".equalsIgnoreCase(paymentResponse.getData().getPayment().getStatus())) {

                log.info("Saga Step Succeeds: Payment processed successfully for Order: {}", order.getId());

                OrderStatus previousStatus = order.getStatus();
                order.setStatus(OrderStatus.ACCEPTED);

                OrderStatusHistory history = OrderStatusHistory.builder()
                        .order(order)
                        .previousStatus(previousStatus)
                        .newStatus(OrderStatus.ACCEPTED)
                        .reason("Saga execution succeeds: payment processed successfully")
                        .build();
                order.addStatusHistory(history);

                Order savedOrder = orderRepository.save(order);

                // Publish PaymentSuccessEvent
                PaymentSuccessEvent successEvent = PaymentSuccessEvent.builder()
                        .eventId(UUID.randomUUID())
                        .eventType("PAYMENT_SUCCESS")
                        .timestamp(LocalDateTime.now())
                        .payload(PaymentSuccessEvent.Payload.builder()
                                .paymentId(paymentResponse.getData().getPayment().getId())
                                .orderId(savedOrder.getId())
                                .amount(savedOrder.getTotalAmount())
                                .currency("USD")
                                .paymentMethod("CREDIT_CARD")
                                .idempotencyKey(idempotencyKey)
                                .build())
                        .build();
                eventProducer.sendPaymentSuccessEvent(successEvent);

                return savedOrder;
            } else {
                String errorMsg = (paymentResponse != null && paymentResponse.getData() != null
                        && paymentResponse.getData().getPayment() != null)
                        ? paymentResponse.getData().getPayment().getErrorMessage()
                        : "Payment processing returned non-success";

                log.warn("Saga Step Fails: Payment unsuccessful for Order: {} - {}", order.getId(), errorMsg);
                return compensate(order, errorMsg, idempotencyKey);
            }
        } catch (Exception e) {
            log.error("Saga Step Fails: Exception occurred during Payment Service call for Order: {}", order.getId(), e);
            return compensate(order, "Payment call failed: " + e.getMessage(), idempotencyKey);
        }
    }

    private Order compensate(Order order, String reason, String idempotencyKey) {
        log.info("Executing Saga Compensation Logic for Order: {} due to: {}", order.getId(), reason);

        OrderStatus previousStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .previousStatus(previousStatus)
                .newStatus(OrderStatus.CANCELLED)
                .reason("Saga compensation executed: " + reason)
                .build();
        order.addStatusHistory(history);

        Order savedOrder = orderRepository.save(order);

        // Publish PaymentFailedEvent
        PaymentFailedEvent failedEvent = PaymentFailedEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PAYMENT_FAILED")
                .timestamp(LocalDateTime.now())
                .payload(PaymentFailedEvent.Payload.builder()
                        .orderId(savedOrder.getId())
                        .amount(savedOrder.getTotalAmount())
                        .currency("USD")
                        .paymentMethod("CREDIT_CARD")
                        .idempotencyKey(idempotencyKey)
                        .errorMessage(reason)
                        .build())
                .build();
        eventProducer.sendPaymentFailedEvent(failedEvent);

        return savedOrder;
    }
}
