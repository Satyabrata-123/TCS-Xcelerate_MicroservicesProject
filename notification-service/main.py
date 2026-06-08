import logging
import sys
import signal
from app.config import Config
from app.service import NotificationService
from app.consumer import NotificationConsumer

# Setup Logging
logging.basicConfig(
    level=Config.LOG_LEVEL,
    format='%(asctime)s [%(levelname)s] %(name)s: %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger("notification-service")

def main():
    logger.info("Starting Notification Service...")
    
    try:
        service = NotificationService()
        consumer = NotificationConsumer(service)
    except Exception as e:
        logger.critical(f"Failed to initialize service or consumer dependencies: {e}", exc_info=True)
        sys.exit(1)

    def handle_signal(signum, frame):
        logger.info(f"Received signal {signum}. Initiating graceful shutdown...")
        consumer.stop()

    signal.signal(signal.SIGINT, handle_signal)
    signal.signal(signal.SIGTERM, handle_signal)

    logger.info("Kafka consumer loop starting. Press CTRL+C to stop.")
    consumer.start()
    logger.info("Notification Service shutdown complete.")

if __name__ == "__main__":
    main()
