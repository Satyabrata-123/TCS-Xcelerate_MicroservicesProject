import { consumer, producer } from '../config/kafka.js';
import paymentService from '../services/paymentService.js';
import { publishPaymentSuccess, publishPaymentFailed } from '../producers/paymentProducer.js';
import logger from '../config/logger.js';
import cryptoModule from 'crypto';

const ORDER_CREATED_TOPIC = 'order-created';
const DLQ_TOPIC = 'order-created-dlq';
const MAX_RETRIES = 3;

async function startOrderConsumer() {
  try {
    logger.info(`Subscribing to topic: ${ORDER_CREATED_TOPIC}`);
    await consumer.subscribe({ topic: ORDER_CREATED_TOPIC, fromBeginning: true });

    await consumer.run({
      eachMessage: async ({ topic, partition, message }) => {
        const messageKey = message.key ? message.key.toString() : null;
        const rawValue = message.value ? message.value.toString() : null;
        
        logger.info(`Received message on ${topic} [partition ${partition}]: key=${messageKey}`);

        let event;
        try {
          event = JSON.parse(rawValue);
        } catch (parseError) {
          logger.error('Failed to parse incoming order-created message JSON:', parseError);
          await sendToDlq(rawValue, messageKey, parseError.message, topic, partition, message.offset);
          return;
        }

        let attempts = 0;
        let success = false;
        let lastError = null;

        while (attempts < MAX_RETRIES && !success) {
          attempts++;
          try {
            await processOrderCreatedEvent(event);
            success = true;
          } catch (error) {
            lastError = error;
            logger.warn(`Attempt ${attempts} to process order-created event failed: ${error.message}`);
            if (attempts < MAX_RETRIES) {
              const delay = attempts * 1000;
              await new Promise(resolve => setTimeout(resolve, delay));
            }
          }
        }

        if (!success) {
          logger.error(`Exhausted all ${MAX_RETRIES} attempts to process order ${event.payload?.orderId || messageKey}. Moving to DLQ.`);
          await sendToDlq(rawValue, messageKey, lastError?.message || 'Unknown processing error', topic, partition, message.offset);
        }
      }
    });
  } catch (error) {
    logger.error('Error in order consumer:', error);
  }
}

async function processOrderCreatedEvent(event) {
  const { orderId, totalAmount } = event.payload;

  if (!orderId || totalAmount === undefined) {
    throw new Error('Invalid order event payload: missing orderId or totalAmount');
  }

  const idempotencyKey = `payment-order-${orderId}`;
  const amount = Number(totalAmount);
  const currency = 'USD';
  const paymentMethod = 'CREDIT_CARD';

  logger.info(`Processing payment for order ${orderId}, amount: ${amount}, idempotencyKey: ${idempotencyKey}`);

  const payment = await paymentService.processPayment({
    orderId,
    amount,
    currency,
    paymentMethod,
    idempotencyKey
  });

  if (payment.status === 'SUCCESS') {
    await publishPaymentSuccess(payment);
  } else {
    await publishPaymentFailed(
      orderId,
      amount,
      currency,
      paymentMethod,
      idempotencyKey,
      payment.errorMessage || 'Payment failed'
    );
  }
}

async function sendToDlq(originalMessageValue, messageKey, errorMessage, originalTopic, originalPartition, originalOffset) {
  try {
    const dlqPayload = {
      eventId: cryptoModule.randomUUID(),
      timestamp: new Date().toISOString(),
      originalTopic,
      originalPartition,
      originalOffset,
      errorMessage,
      payload: originalMessageValue ? JSON.parse(originalMessageValue) : null
    };

    logger.info(`Publishing failed message to DLQ topic: ${DLQ_TOPIC}`);
    await producer.send({
      topic: DLQ_TOPIC,
      messages: [
        {
          key: messageKey,
          value: JSON.stringify(dlqPayload),
          headers: {
            'x-dead-letter-reason': errorMessage,
            'x-dead-letter-topic': originalTopic,
            'x-dead-letter-partition': originalPartition.toString(),
            'x-dead-letter-offset': originalOffset
          }
        }
      ]
    });
  } catch (error) {
    logger.error('CRITICAL: Failed to publish message to DLQ:', error);
  }
}

export { startOrderConsumer };
