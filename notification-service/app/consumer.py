import json
import logging
from confluent_kafka import Consumer, KafkaError, KafkaException
from .config import Config
from .service import NotificationService
from .dto import OrderCreatedEvent, PaymentSuccessEvent, PaymentFailedEvent, OrderDeliveredEvent

logger = logging.getLogger(__name__)

class NotificationConsumer:
    def __init__(self, service: NotificationService):
        self.service = service
        
        consumer_config = {
            'bootstrap.servers': Config.KAFKA_BOOTSTRAP_SERVERS,
            'group.id': Config.KAFKA_GROUP_ID,
            'client.id': Config.KAFKA_CLIENT_ID,
            'auto.offset.reset': 'earliest',
            'enable.auto.commit': False,  # Manual commit for at-least-once guarantee
        }
        
        logger.info(f"Initializing Kafka Consumer with config: {consumer_config}")
        self.consumer = Consumer(consumer_config)
        self.topics = ['order-created', 'payment-success', 'payment-failed', 'order-delivered']
        self.running = False

    def start(self):
        logger.info(f"Subscribing to topics: {self.topics}")
        self.consumer.subscribe(self.topics)
        self.running = True
        
        try:
            while self.running:
                msg = self.consumer.poll(timeout=1.0)
                if msg is None:
                    continue

                if msg.error():
                    if msg.error().code() == KafkaError._PARTITION_EOF:
                        logger.debug(f"Reached end of partition: {msg.topic()} [{msg.partition()}] at offset {msg.offset()}")
                    else:
                        raise KafkaException(msg.error())
                else:
                    topic = msg.topic()
                    partition = msg.partition()
                    offset = msg.offset()
                    key = msg.key().decode('utf-8') if msg.key() else None
                    value = msg.value().decode('utf-8') if msg.value() else None

                    logger.debug(f"Received message on topic {topic} [partition {partition}] at offset {offset}: key={key}")

                    try:
                        self.process_message(topic, value)
                        self.consumer.commit(msg, asynchronous=True)
                    except Exception as e:
                        logger.error(f"Error processing message from topic {topic} (partition={partition}, offset={offset}): {e}", exc_info=True)
        except KeyboardInterrupt:
            logger.info("Consumer loop interrupted by user.")
        finally:
            logger.info("Closing Kafka consumer...")
            self.consumer.close()
            logger.info("Kafka consumer closed.")

    def stop(self):
        self.running = False

    def process_message(self, topic: str, value: str):
        if not value:
            logger.warning(f"Received empty message on topic: {topic}")
            return

        try:
            data = json.loads(value)
        except json.JSONDecodeError as e:
            logger.error(f"Failed to parse event JSON: {e}")
            return

        event_id = data.get("eventId")
        if not event_id:
            logger.warning("Received event without eventId, skipping idempotency check.")
            return

        if self.service.is_duplicate(event_id):
            logger.info(f"Duplicate event detected: {event_id}. Skipping processing.")
            return

        # Dispatch based on topic and validate with DTOs
        if topic == 'order-created':
            event = OrderCreatedEvent.model_validate(data)
            self.service.handle_order_created(event.payload)
        elif topic == 'payment-success':
            event = PaymentSuccessEvent.model_validate(data)
            self.service.handle_payment_success(event.payload)
        elif topic == 'payment-failed':
            event = PaymentFailedEvent.model_validate(data)
            self.service.handle_payment_failed(event.payload)
        elif topic == 'order-delivered':
            event = OrderDeliveredEvent.model_validate(data)
            self.service.handle_order_delivered(event.payload)
        else:
            logger.warning(f"Unhandled topic: {topic}")
