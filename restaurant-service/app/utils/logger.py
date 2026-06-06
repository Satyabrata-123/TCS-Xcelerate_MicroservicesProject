import logging
import sys
from app.config import settings

def setup_logging():
    log_level = getattr(logging, settings.LOG_LEVEL.upper(), logging.INFO)
    
    # Configure root logger
    logging.basicConfig(
        level=log_level,
        format="[%(asctime)s] %(levelname)s in %(module)s: %(message)s",
        handlers=[
            logging.StreamHandler(sys.stdout)
        ]
    )
    
    # Return custom logger for the app
    app_logger = logging.getLogger("restaurant_service")
    app_logger.setLevel(log_level)
    return app_logger

logger = setup_logging()
