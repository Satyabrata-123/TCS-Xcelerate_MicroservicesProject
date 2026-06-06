from bson import ObjectId
from typing import Any, Annotated, List, Optional
from pydantic import BaseModel, Field
from pydantic_core import core_schema

class ObjectIdPydanticAnnotation:
    @classmethod
    def __get_pydantic_core_schema__(
        cls, source_type: Any, handler: Any
    ) -> core_schema.CoreSchema:
        return core_schema.json_or_python_schema(
            json_schema=core_schema.str_schema(),
            python_schema=core_schema.union_schema([
                core_schema.is_instance_schema(ObjectId),
                core_schema.chain_schema([
                    core_schema.str_schema(),
                    core_schema.no_info_plain_validator_function(cls.validate),
                ]),
            ]),
            serialization=core_schema.plain_serializer_function_ser_schema(
                lambda x: str(x)
            ),
        )

    @classmethod
    def validate(cls, v: Any) -> ObjectId:
        if not ObjectId.is_valid(v):
            raise ValueError("Invalid ObjectId")
        return ObjectId(v)

PyObjectId = Annotated[ObjectId, ObjectIdPydanticAnnotation]

class MenuItemModel(BaseModel):
    id: str = Field(default_factory=lambda: str(ObjectId()))
    name: str
    description: Optional[str] = None
    price: float
    category: str
    is_available: bool = True

    model_config = {
        "populate_by_name": True,
        "arbitrary_types_allowed": True,
    }

class RestaurantModel(BaseModel):
    id: Optional[PyObjectId] = Field(default=None, alias="_id")
    name: str
    description: Optional[str] = None
    cuisine_type: str
    address: str
    phone: str
    rating: Optional[float] = Field(default=0.0, ge=0.0, le=5.0)
    menu: List[MenuItemModel] = Field(default_factory=list)

    model_config = {
        "populate_by_name": True,
        "arbitrary_types_allowed": True,
    }
