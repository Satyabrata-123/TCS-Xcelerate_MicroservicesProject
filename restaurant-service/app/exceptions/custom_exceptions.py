from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
from app.utils.logger import logger

class RestaurantException(Exception):
    def __init__(self, message: str, status_code: int = 400):
        self.message = message
        self.status_code = status_code
        super().__init__(self.message)

class RestaurantNotFoundException(RestaurantException):
    def __init__(self, message: str = "Restaurant not found"):
        super().__init__(message, status_code=404)

class MenuItemNotFoundException(RestaurantException):
    def __init__(self, message: str = "Menu item not found"):
        super().__init__(message, status_code=404)

class DatabaseException(RestaurantException):
    def __init__(self, message: str = "A database error occurred"):
        super().__init__(message, status_code=500)

def register_exception_handlers(app: FastAPI):
    @app.exception_handler(RestaurantException)
    async def restaurant_exception_handler(request: Request, exc: RestaurantException):
        logger.error(f"Domain Exception on {request.url.path}: {exc.message}")
        return JSONResponse(
            status_code=exc.status_code,
            content={"detail": exc.message}
        )

    @app.exception_handler(RequestValidationError)
    async def validation_exception_handler(request: Request, exc: RequestValidationError):
        logger.warning(f"Validation Error on {request.url.path}: {exc.errors()}")
        return JSONResponse(
            status_code=422,
            content={"detail": exc.errors()}
        )

    @app.exception_handler(Exception)
    async def general_exception_handler(request: Request, exc: Exception):
        logger.critical(f"Unhandled Exception on {request.url.path}: {str(exc)}", exc_info=True)
        return JSONResponse(
            status_code=500,
            content={"detail": "An internal server error occurred."}
        )
