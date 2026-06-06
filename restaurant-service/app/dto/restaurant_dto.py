from pydantic import BaseModel, Field, BeforeValidator
from typing import List, Optional, Annotated
from app.dto.menu_dto import MenuItemResponse

StrObjectId = Annotated[str, BeforeValidator(str)]

class RestaurantCreateRequest(BaseModel):
    name: str = Field(..., min_length=1, max_length=100, description="Name of the restaurant")
    description: Optional[str] = Field(None, max_length=500, description="Description of the restaurant")
    cuisine_type: str = Field(..., min_length=1, max_length=100, description="Cuisine type (e.g. Italian, Indian, Chinese)")
    address: str = Field(..., min_length=1, max_length=200, description="Address of the restaurant")
    phone: str = Field(..., min_length=5, max_length=20, description="Phone number")
    rating: Optional[float] = Field(0.0, ge=0.0, le=5.0, description="Restaurant rating between 0 and 5")

class RestaurantUpdateRequest(BaseModel):
    name: Optional[str] = Field(None, min_length=1, max_length=100)
    description: Optional[str] = Field(None, max_length=500)
    cuisine_type: Optional[str] = Field(None, min_length=1, max_length=100)
    address: Optional[str] = Field(None, min_length=1, max_length=200)
    phone: Optional[str] = Field(None, min_length=5, max_length=20)
    rating: Optional[float] = Field(None, ge=0.0, le=5.0)

class RestaurantResponse(BaseModel):
    id: StrObjectId

    name: str
    description: Optional[str] = None
    cuisine_type: str
    address: str
    phone: str
    rating: float
    menu: List[MenuItemResponse] = Field(default_factory=list)

    model_config = {
        "from_attributes": True,
        "populate_by_name": True
    }
