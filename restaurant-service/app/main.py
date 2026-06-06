from contextlib import asynccontextmanager
import time
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware

from app.config import settings
from app.database import connect_to_mongo, close_mongo_connection
from app.exceptions.custom_exceptions import register_exception_handlers
from app.routers import restaurant_router
from app.utils.logger import logger

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup: Initialize DB Connection
    await connect_to_mongo()
    yield
    # Shutdown: Close DB Connection
    await close_mongo_connection()

app = FastAPI(
    title=settings.PROJECT_NAME,
    description="A microservice handling restaurant lifecycle operations, menu management, and searches using FastAPI and MongoDB (Motor).",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan
)

# CORS Middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Global logging and performance instrumentation middleware
@app.middleware("http")
async def log_requests(request: Request, call_next):
    start_time = time.time()
    
    # Log incoming request
    logger.info(f"Incoming request: {request.method} {request.url.path}")
    
    response = await call_next(request)
    
    process_time = (time.time() - start_time) * 1000
    logger.info(f"Completed request: {request.method} {request.url.path} - Status: {response.status_code} - Duration: {process_time:.2f}ms")
    
    return response

# Register Exception Handlers
register_exception_handlers(app)

# Register Routers
app.include_router(restaurant_router.router)

@app.get("/health", tags=["Health"])
async def health_check():
    from app.database import db_instance
    db_status = "unknown"
    if db_instance.client:
        try:
            # Send a ping to confirm a successful connection
            await db_instance.client.admin.command('ping')
            db_status = "connected"
        except Exception as e:
            logger.error(f"Health check MongoDB ping failed: {e}")
            db_status = "disconnected"
    return {
        "status": "healthy" if db_status == "connected" else "degraded",
        "service": settings.PROJECT_NAME,
        "database": db_status
    }
