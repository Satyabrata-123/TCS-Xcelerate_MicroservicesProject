from motor.motor_asyncio import AsyncIOMotorDatabase
from bson import ObjectId
from typing import List, Optional
from app.models.restaurant import RestaurantModel, MenuItemModel

class RestaurantRepository:
    def __init__(self, db: AsyncIOMotorDatabase):
        self.collection = db["restaurants"]

    async def create(self, restaurant: RestaurantModel) -> RestaurantModel:
        restaurant_dict = restaurant.model_dump(by_alias=True, exclude_none=True)
        if "_id" in restaurant_dict and restaurant_dict["_id"] is None:
            del restaurant_dict["_id"]
        
        result = await self.collection.insert_one(restaurant_dict)
        restaurant.id = result.inserted_id
        return restaurant

    async def get_by_id(self, id: ObjectId) -> Optional[RestaurantModel]:
        doc = await self.collection.find_one({"_id": id})
        if doc:
            return RestaurantModel.model_validate(doc)
        return None

    async def update(self, id: ObjectId, update_data: dict) -> Optional[RestaurantModel]:
        if not update_data:
            return await self.get_by_id(id)
        
        await self.collection.update_one(
            {"_id": id},
            {"$set": update_data}
        )
        return await self.get_by_id(id)

    async def delete(self, id: ObjectId) -> bool:
        result = await self.collection.delete_one({"_id": id})
        return result.deleted_count > 0

    async def search(self, filter_query: dict, skip: int = 0, limit: int = 100) -> List[RestaurantModel]:
        cursor = self.collection.find(filter_query).skip(skip).limit(limit)
        docs = await cursor.to_list(length=limit)
        return [RestaurantModel.model_validate(doc) for doc in docs]

    async def add_menu_item(self, restaurant_id: ObjectId, menu_item: MenuItemModel) -> Optional[RestaurantModel]:
        item_dict = menu_item.model_dump()
        result = await self.collection.update_one(
            {"_id": restaurant_id},
            {"$push": {"menu": item_dict}}
        )
        if result.modified_count > 0 or result.matched_count > 0:
            return await self.get_by_id(restaurant_id)
        return None

    async def remove_menu_item(self, restaurant_id: ObjectId, menu_item_id: str) -> Optional[RestaurantModel]:
        result = await self.collection.update_one(
            {"_id": restaurant_id},
            {"$pull": {"menu": {"id": menu_item_id}}}
        )
        if result.modified_count > 0 or result.matched_count > 0:
            return await self.get_by_id(restaurant_id)
        return None
