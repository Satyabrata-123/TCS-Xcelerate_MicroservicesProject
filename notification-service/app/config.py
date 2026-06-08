import os
from dotenv import load_dotenv

load_dotenv()

class Config:
    # Kafka Configuration
    KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:29092")
    KAFKA_GROUP_ID = os.getenv("KAFKA_GROUP_ID", "notification-group")
    KAFKA_CLIENT_ID = os.getenv("KAFKA_CLIENT_ID", "notification-service")
    
    # Redis Configuration
    REDIS_HOST = os.getenv("REDIS_HOST", "localhost")
    REDIS_PORT = int(os.getenv("REDIS_PORT", 6379))
    REDIS_DB = int(os.getenv("REDIS_DB", 0))
    REDIS_PASSWORD = os.getenv("REDIS_PASSWORD", None)
    
    # Application Configuration
    PORT = int(os.getenv("PORT", 5005))
    LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")
