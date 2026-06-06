from pydantic import BaseModel, Field
from typing import Optional

class MenuItemCreateRequest(BaseModel):
    name: str = Field(..., min_length=1, max_length=100, description="Name of the menu item")
    description: Optional[str] = Field(None, max_length=300, description="Description of the item")
    price: float = Field(..., gt=0.0, description="Price of the item (must be greater than 0)")
    category: str = Field(..., min_length=1, max_length=50, description="Category (e.g. Starter, Main, Dessert, Drink)")
    is_available: bool = Field(True, description="Availability status")

class MenuItemResponse(BaseModel):
    id: str
    name: str
    description: Optional[str] = None
    price: float
    category: str
    is_available: bool

    model_config = {
        "from_attributes": True
    }
