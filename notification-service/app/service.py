import logging
import redis
from .config import Config
from .dto import OrderCreatedPayload, PaymentSuccessPayload, PaymentFailedPayload, OrderDeliveredPayload

logger = logging.getLogger(__name__)

class NotificationService:
    def __init__(self):
        logger.info(f"Connecting to Redis at {Config.REDIS_HOST}:{Config.REDIS_PORT}...")
        self.redis_client = redis.Redis(
            host=Config.REDIS_HOST,
            port=Config.REDIS_PORT,
            db=Config.REDIS_DB,
            password=Config.REDIS_PASSWORD,
            decode_responses=True
        )
        try:
            self.redis_client.ping()
            logger.info("Connected to Redis successfully.")
        except Exception as e:
            logger.error(f"Failed to connect to Redis: {e}")
            raise e

    def is_duplicate(self, event_id: str) -> bool:
        """
        Check if an event has already been processed to ensure idempotency.
        Saves the event_id in Redis with a 24-hour TTL.
        """
        key = f"processed_event:{event_id}"
        # set with ex=86400 (24h TTL) and nx=True (set only if it doesn't exist)
        is_new = self.redis_client.set(key, "processed", ex=86400, nx=True)
        return not is_new

    def handle_order_created(self, payload: OrderCreatedPayload):
        order_id = payload.orderId
        customer_id = payload.customerId
        total_amount = payload.totalAmount
        
        sms_message = f"Your order {order_id} for ${total_amount} has been placed successfully and is pending payment."
        logger.info(f"[SMS SENT] to Customer {customer_id}: \"{sms_message}\"")

    def handle_payment_success(self, payload: PaymentSuccessPayload):
        order_id = payload.orderId
        amount = payload.amount
        
        email_body = f"Payment of ${amount} for order {order_id} was successful. Your order is now accepted and being prepared!"
        logger.info(f"[EMAIL SENT] to Customer: \"{email_body}\"")

    def handle_payment_failed(self, payload: PaymentFailedPayload):
        order_id = payload.orderId
        amount = payload.amount
        error_msg = payload.errorMessage
        
        alert_msg = f"Warning: Payment of ${amount} for order {order_id} failed. Reason: {error_msg}. Please check your payment details."
        logger.info(f"[SMS & EMAIL ALERT SENT] to Customer: \"{alert_msg}\"")

    def handle_order_delivered(self, payload: OrderDeliveredPayload):
        order_id = payload.orderId
        customer_id = payload.customerId
        
        push_msg = f"Delivered! Your order {order_id} has been successfully delivered. Bon appétit!"
        logger.info(f"[PUSH NOTIFICATION SENT] to Customer {customer_id}: \"{push_msg}\"")
