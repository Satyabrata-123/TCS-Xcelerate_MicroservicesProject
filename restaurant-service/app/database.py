from motor.motor_asyncio import AsyncIOMotorClient
from app.config import settings
from app.utils.logger import logger

class Database:
    client: AsyncIOMotorClient = None
    db = None

db_instance = Database()

async def connect_to_mongo():
    logger.info(f"Connecting to MongoDB at {settings.MONGO_URI}...")
    db_instance.client = AsyncIOMotorClient(settings.MONGO_URI)
    db_instance.db = db_instance.client[settings.MONGO_DB_NAME]
    logger.info("Successfully connected to MongoDB.")

async def close_mongo_connection():
    if db_instance.client:
        logger.info("Closing MongoDB connection...")
        db_instance.client.close()
        logger.info("MongoDB connection closed.")

def get_database():
    if db_instance.db is None:
        raise RuntimeError("Database not initialized. Ensure connect_to_mongo() was called.")
    return db_instance.db
