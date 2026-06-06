from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    PROJECT_NAME: str = "Restaurant Service"
    MONGO_URI: str = "mongodb://localhost:27017"
    MONGO_DB_NAME: str = "restaurant_db"
    PORT: int = 8001
    LOG_LEVEL: str = "INFO"

    # Eureka Configurations
    EUREKA_HOST: str = "localhost"
    EUREKA_PORT: int = 8761
    EUREKA_APP_NAME: str = "RESTAURANT-SERVICE"
    EUREKA_INSTANCE_HOST: str = "localhost"
    EUREKA_INSTANCE_IP: str = "127.0.0.1"

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore"
    )

settings = Settings()
