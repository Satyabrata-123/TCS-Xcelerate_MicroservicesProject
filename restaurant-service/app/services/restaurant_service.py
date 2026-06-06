from typing import List, Optional
from bson import ObjectId
from bson.errors import InvalidId

from app.repositories.restaurant_repository import RestaurantRepository
from app.models.restaurant import RestaurantModel, MenuItemModel
from app.dto.restaurant_dto import RestaurantCreateRequest, RestaurantUpdateRequest, RestaurantResponse
from app.dto.menu_dto import MenuItemCreateRequest
from app.exceptions.custom_exceptions import RestaurantNotFoundException, MenuItemNotFoundException
from app.utils.logger import logger

class RestaurantService:
    def __init__(self, repository: RestaurantRepository):
        self.repository = repository

    def _parse_id(self, id_str: str) -> ObjectId:
        try:
            return ObjectId(id_str)
        except (InvalidId, TypeError):
            logger.warning(f"Invalid ObjectId format provided: {id_str}")
            raise RestaurantNotFoundException(f"Restaurant not found with id: {id_str}")

    async def create_restaurant(self, request: RestaurantCreateRequest) -> RestaurantResponse:
        logger.info(f"Creating new restaurant with name: {request.name}")
        model = RestaurantModel(
            name=request.name,
            description=request.description,
            cuisine_type=request.cuisine_type,
            address=request.address,
            phone=request.phone,
            rating=request.rating,
            menu=[]
        )
        created_model = await self.repository.create(model)
        logger.info(f"Successfully created restaurant with ID: {created_model.id}")
        return RestaurantResponse.model_validate(created_model)

    async def get_restaurant(self, id_str: str) -> RestaurantResponse:
        logger.info(f"Retrieving restaurant with ID: {id_str}")
        obj_id = self._parse_id(id_str)
        model = await self.repository.get_by_id(obj_id)
        if not model:
            logger.warning(f"Restaurant with ID {id_str} not found")
            raise RestaurantNotFoundException(f"Restaurant not found with id: {id_str}")
        return RestaurantResponse.model_validate(model)

    async def update_restaurant(self, id_str: str, request: RestaurantUpdateRequest) -> RestaurantResponse:
        logger.info(f"Updating restaurant with ID: {id_str}")
        obj_id = self._parse_id(id_str)
        
        # Check if restaurant exists
        existing = await self.repository.get_by_id(obj_id)
        if not existing:
            logger.warning(f"Restaurant with ID {id_str} not found for update")
            raise RestaurantNotFoundException(f"Restaurant not found with id: {id_str}")
            
        update_data = request.model_dump(exclude_unset=True)
        updated_model = await self.repository.update(obj_id, update_data)
        logger.info(f"Successfully updated restaurant with ID: {id_str}")
        return RestaurantResponse.model_validate(updated_model)

    async def delete_restaurant(self, id_str: str) -> None:
        logger.info(f"Deleting restaurant with ID: {id_str}")
        obj_id = self._parse_id(id_str)
        deleted = await self.repository.delete(obj_id)
        if not deleted:
            logger.warning(f"Restaurant with ID {id_str} not found for deletion")
            raise RestaurantNotFoundException(f"Restaurant not found with id: {id_str}")
        logger.info(f"Successfully deleted restaurant with ID: {id_str}")

    async def search_restaurants(
        self,
        name: Optional[str] = None,
        cuisine_type: Optional[str] = None,
        min_rating: Optional[float] = None,
        skip: int = 0,
        limit: int = 100
    ) -> List[RestaurantResponse]:
        logger.info(f"Searching restaurants with filters - name: {name}, cuisine_type: {cuisine_type}, min_rating: {min_rating}")
        
        filter_query = {}
        if name:
            filter_query["name"] = {"$regex": name, "$options": "i"}
        if cuisine_type:
            filter_query["cuisine_type"] = {"$regex": cuisine_type, "$options": "i"}
        if min_rating is not None:
            filter_query["rating"] = {"$gte": min_rating}

        models = await self.repository.search(filter_query, skip, limit)
        return [RestaurantResponse.model_validate(m) for m in models]

    async def add_menu_item(self, restaurant_id_str: str, request: MenuItemCreateRequest) -> RestaurantResponse:
        logger.info(f"Adding menu item '{request.name}' to restaurant ID: {restaurant_id_str}")
        obj_id = self._parse_id(restaurant_id_str)
        
        # Check if restaurant exists
        existing = await self.repository.get_by_id(obj_id)
        if not existing:
            logger.warning(f"Restaurant with ID {restaurant_id_str} not found to add menu item")
            raise RestaurantNotFoundException(f"Restaurant not found with id: {restaurant_id_str}")
            
        menu_item_model = MenuItemModel(
            name=request.name,
            description=request.description,
            price=request.price,
            category=request.category,
            is_available=request.is_available
        )
        
        updated_restaurant = await self.repository.add_menu_item(obj_id, menu_item_model)
        logger.info(f"Successfully added menu item to restaurant ID: {restaurant_id_str}")
        return RestaurantResponse.model_validate(updated_restaurant)

    async def remove_menu_item(self, restaurant_id_str: str, menu_item_id: str) -> RestaurantResponse:
        logger.info(f"Removing menu item ID '{menu_item_id}' from restaurant ID: {restaurant_id_str}")
        obj_id = self._parse_id(restaurant_id_str)
        
        # Check if restaurant exists
        existing = await self.repository.get_by_id(obj_id)
        if not existing:
            logger.warning(f"Restaurant with ID {restaurant_id_str} not found to remove menu item")
            raise RestaurantNotFoundException(f"Restaurant not found with id: {restaurant_id_str}")
            
        # Verify that the menu item exists in the restaurant
        menu_item_exists = any(item.id == menu_item_id for item in existing.menu)
        if not menu_item_exists:
            logger.warning(f"Menu item ID {menu_item_id} not found in restaurant ID {restaurant_id_str}")
            raise MenuItemNotFoundException(f"Menu item not found with id: {menu_item_id}")
            
        updated_restaurant = await self.repository.remove_menu_item(obj_id, menu_item_id)
        logger.info(f"Successfully removed menu item ID {menu_item_id} from restaurant ID: {restaurant_id_str}")
        return RestaurantResponse.model_validate(updated_restaurant)
