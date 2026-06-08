package com.fdp.orderservice.service;

import com.fdp.orderservice.dto.event.PaymentFailedEvent;
import com.fdp.orderservice.dto.event.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final OrderService orderService;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 2000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_DELAY_VALUE,
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            dltTopicSuffix = "-dlq"
    )
    @KafkaListener(topics = "payment-success", groupId = "order-group")
    public void handlePaymentSuccess(PaymentSuccessEvent event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.info("Received PaymentSuccessEvent from topic {}: {}", topic, event);
        UUID orderId = event.getPayload().getOrderId();
        orderService.acceptOrder(orderId);
        log.info("Successfully processed payment success for order: {}", orderId);
    }

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 2000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_DELAY_VALUE,
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            dltTopicSuffix = "-dlq"
    )
    @KafkaListener(topics = "payment-failed", groupId = "order-group")
    public void handlePaymentFailed(PaymentFailedEvent event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.info("Received PaymentFailedEvent from topic {}: {}", topic, event);
        UUID orderId = event.getPayload().getOrderId();
        String reason = event.getPayload().getErrorMessage();
        orderService.cancelOrder(orderId, reason);
        log.info("Successfully processed payment failure for order: {}", orderId);
    }

    @DltHandler
    public void handleDlt(Object message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("Message {} forwarded to DLT topic: {}", message, topic);
    }
}
