import { producer } from '../config/kafka.js';
import logger from '../config/logger.js';
import cryptoModule from 'crypto';

function generateEventId() {
  return cryptoModule.randomUUID();
}

async function publishPaymentSuccess(payment) {
  const event = {
    eventId: generateEventId(),
    eventType: 'PAYMENT_SUCCESS',
    timestamp: new Date().toISOString(),
    payload: {
      paymentId: payment.id,
      orderId: payment.orderId,
      amount: payment.amount,
      currency: payment.currency,
      paymentMethod: payment.paymentMethod,
      idempotencyKey: payment.idempotencyKey
    }
  };

  try {
    logger.info(`Publishing PAYMENT_SUCCESS event for order ${payment.orderId}`);
    await producer.send({
      topic: 'payment-success',
      messages: [
        {
          key: payment.orderId.toString(),
          value: JSON.stringify(event)
        }
      ]
    });
  } catch (error) {
    logger.error('Failed to publish PAYMENT_SUCCESS event:', error);
  }
}

async function publishPaymentFailed(orderId, amount, currency, paymentMethod, idempotencyKey, errorMessage) {
  const event = {
    eventId: generateEventId(),
    eventType: 'PAYMENT_FAILED',
    timestamp: new Date().toISOString(),
    payload: {
      orderId,
      amount,
      currency,
      paymentMethod,
      idempotencyKey,
      errorMessage
    }
  };

  try {
    logger.info(`Publishing PAYMENT_FAILED event for order ${orderId}`);
    await producer.send({
      topic: 'payment-failed',
      messages: [
        {
          key: orderId.toString(),
          value: JSON.stringify(event)
        }
      ]
    });
  } catch (error) {
    logger.error('Failed to publish PAYMENT_FAILED event:', error);
  }
}

export { publishPaymentSuccess, publishPaymentFailed };
