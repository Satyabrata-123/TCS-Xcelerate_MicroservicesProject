from contextlib import asynccontextmanager
import time
from fastapi import FastAPI, Request, WebSocket
from fastapi.middleware.cors import CORSMiddleware

from app.config import settings
from app.database import connect_to_mongo, close_mongo_connection
from app.exceptions.custom_exceptions import register_exception_handlers
from app.routers import restaurant_router
from app.utils.logger import logger
import py_eureka_client.eureka_client as eureka_client

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup: Initialize DB Connection
    await connect_to_mongo()
    
    # Initialize Eureka Client Registration
    logger.info(f"Registering with Eureka server at http://{settings.EUREKA_HOST}:{settings.EUREKA_PORT}/eureka/...")
    try:
        await eureka_client.init_async(
            eureka_server=f"http://{settings.EUREKA_HOST}:{settings.EUREKA_PORT}/eureka",
            app_name=settings.EUREKA_APP_NAME,
            instance_port=settings.PORT,
            instance_host=settings.EUREKA_INSTANCE_HOST,
            instance_ip=settings.EUREKA_INSTANCE_IP
        )
        logger.info(f"Successfully registered with Eureka registry as {settings.EUREKA_APP_NAME}")
    except Exception as e:
        logger.error(f"Failed to register with Eureka: {e}")
        
    yield
    
    # Shutdown: Deregister from Eureka & Close DB Connection
    try:
        logger.info("Deregistering from Eureka server...")
        await eureka_client.stop_async()
        logger.info("Successfully deregistered from Eureka.")
    except Exception as e:
        logger.error(f"Failed to deregister from Eureka: {e}")
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

@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    await websocket.accept()
    try:
        while True:
            await websocket.receive_text()
    except Exception:
        pass

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
if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host="0.0.0.0", port=8001, reload=True)
