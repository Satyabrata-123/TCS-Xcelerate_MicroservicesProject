package com.fdp.orderservice.service;

import com.fdp.orderservice.dto.event.OrderCreatedEvent;
import com.fdp.orderservice.dto.event.OrderDeliveredEvent;
import com.fdp.orderservice.dto.event.PaymentFailedEvent;
import com.fdp.orderservice.dto.event.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String ORDER_CREATED_TOPIC = "order-created";
    private static final String ORDER_DELIVERED_TOPIC = "order-delivered";
    private static final String PAYMENT_SUCCESS_TOPIC = "payment-success";
    private static final String PAYMENT_FAILED_TOPIC = "payment-failed";

    public void sendOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Publishing OrderCreatedEvent to topic {} for order: {}", ORDER_CREATED_TOPIC, event.getPayload().getOrderId());
        kafkaTemplate.send(ORDER_CREATED_TOPIC, event.getPayload().getOrderId().toString(), event);
    }

    public void sendOrderDeliveredEvent(OrderDeliveredEvent event) {
        log.info("Publishing OrderDeliveredEvent to topic {} for order: {}", ORDER_DELIVERED_TOPIC, event.getPayload().getOrderId());
        kafkaTemplate.send(ORDER_DELIVERED_TOPIC, event.getPayload().getOrderId().toString(), event);
    }

    public void sendPaymentSuccessEvent(PaymentSuccessEvent event) {
        log.info("Publishing PaymentSuccessEvent to topic {} for order: {}", PAYMENT_SUCCESS_TOPIC, event.getPayload().getOrderId());
        kafkaTemplate.send(PAYMENT_SUCCESS_TOPIC, event.getPayload().getOrderId().toString(), event);
    }

    public void sendPaymentFailedEvent(PaymentFailedEvent event) {
        log.info("Publishing PaymentFailedEvent to topic {} for order: {}", PAYMENT_FAILED_TOPIC, event.getPayload().getOrderId());
        kafkaTemplate.send(PAYMENT_FAILED_TOPIC, event.getPayload().getOrderId().toString(), event);
    }
}
