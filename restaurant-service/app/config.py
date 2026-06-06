from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    PROJECT_NAME: str = "Restaurant Service"
    MONGO_URI: str = "mongodb://localhost:27017"
    MONGO_DB_NAME: str = "restaurant_db"
    PORT: int = 8000
    LOG_LEVEL: str = "INFO"

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore"
    )

settings = Settings()
