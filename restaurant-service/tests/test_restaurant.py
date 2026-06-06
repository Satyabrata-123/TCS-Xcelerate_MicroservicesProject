import unittest
from unittest.mock import AsyncMock, MagicMock
from bson import ObjectId

from app.services.restaurant_service import RestaurantService
from app.repositories.restaurant_repository import RestaurantRepository
from app.dto.restaurant_dto import RestaurantCreateRequest, RestaurantUpdateRequest
from app.dto.menu_dto import MenuItemCreateRequest
from app.exceptions.custom_exceptions import RestaurantNotFoundException, MenuItemNotFoundException
from app.models.restaurant import RestaurantModel, MenuItemModel

class TestRestaurantService(unittest.IsolatedAsyncioTestCase):
    def setUp(self):
        self.mock_repo = MagicMock(spec=RestaurantRepository)
        self.service = RestaurantService(self.mock_repo)

    async def test_create_restaurant(self):
        # Arrange
        req = RestaurantCreateRequest(
            name="Pizza Palace",
            description="Best pizza in town",
            cuisine_type="Italian",
            address="123 Main St",
            phone="123-456-7890",
            rating=4.5
        )
        created_id = ObjectId()
        mock_model = RestaurantModel(
            id=created_id,
            name=req.name,
            description=req.description,
            cuisine_type=req.cuisine_type,
            address=req.address,
            phone=req.phone,
            rating=req.rating,
            menu=[]
        )
        self.mock_repo.create = AsyncMock(return_value=mock_model)

        # Act
        response = await self.service.create_restaurant(req)

        # Assert
        self.assertEqual(response.id, str(created_id))
        self.assertEqual(response.name, "Pizza Palace")
        self.mock_repo.create.assert_called_once()

    async def test_get_restaurant_success(self):
        # Arrange
        restaurant_id = ObjectId()
        mock_model = RestaurantModel(
            id=restaurant_id,
            name="Burger Joint",
            cuisine_type="American",
            address="456 Elm St",
            phone="987-654-3210",
            rating=4.0,
            menu=[]
        )
        self.mock_repo.get_by_id = AsyncMock(return_value=mock_model)

        # Act
        response = await self.service.get_restaurant(str(restaurant_id))

        # Assert
        self.assertEqual(response.name, "Burger Joint")
        self.assertEqual(response.id, str(restaurant_id))
        self.mock_repo.get_by_id.assert_called_once_with(restaurant_id)

    async def test_get_restaurant_not_found(self):
        # Arrange
        restaurant_id = ObjectId()
        self.mock_repo.get_by_id = AsyncMock(return_value=None)

        # Act & Assert
        with self.assertRaises(RestaurantNotFoundException):
            await self.service.get_restaurant(str(restaurant_id))

    async def test_update_restaurant_success(self):
        # Arrange
        restaurant_id = ObjectId()
        req = RestaurantUpdateRequest(name="New Burger Joint", rating=4.8)
        
        existing_model = RestaurantModel(
            id=restaurant_id,
            name="Burger Joint",
            cuisine_type="American",
            address="456 Elm St",
            phone="987-654-3210",
            rating=4.0,
            menu=[]
        )
        updated_model = RestaurantModel(
            id=restaurant_id,
            name="New Burger Joint",
            cuisine_type="American",
            address="456 Elm St",
            phone="987-654-3210",
            rating=4.8,
            menu=[]
        )
        
        self.mock_repo.get_by_id = AsyncMock(return_value=existing_model)
        self.mock_repo.update = AsyncMock(return_value=updated_model)

        # Act
        response = await self.service.update_restaurant(str(restaurant_id), req)

        # Assert
        self.assertEqual(response.name, "New Burger Joint")
        self.assertEqual(response.rating, 4.8)
        self.mock_repo.update.assert_called_once_with(restaurant_id, {"name": "New Burger Joint", "rating": 4.8})

    async def test_add_menu_item_success(self):
        # Arrange
        restaurant_id = ObjectId()
        req = MenuItemCreateRequest(
            name="Margherita Pizza",
            description="Classic cheese pizza",
            price=12.99,
            category="Main"
        )
        
        existing_model = RestaurantModel(
            id=restaurant_id,
            name="Pizza Palace",
            cuisine_type="Italian",
            address="123 Main St",
            phone="123-456-7890",
            rating=4.5,
            menu=[]
        )
        
        added_item = MenuItemModel(
            id="some-uuid",
            name=req.name,
            description=req.description,
            price=req.price,
            category=req.category,
            is_available=True
        )
        
        updated_model = RestaurantModel(
            id=restaurant_id,
            name="Pizza Palace",
            cuisine_type="Italian",
            address="123 Main St",
            phone="123-456-7890",
            rating=4.5,
            menu=[added_item]
        )
        
        self.mock_repo.get_by_id = AsyncMock(return_value=existing_model)
        self.mock_repo.add_menu_item = AsyncMock(return_value=updated_model)

        # Act
        response = await self.service.add_menu_item(str(restaurant_id), req)

        # Assert
        self.assertEqual(len(response.menu), 1)
        self.assertEqual(response.menu[0].name, "Margherita Pizza")
        self.mock_repo.add_menu_item.assert_called_once()

    async def test_remove_menu_item_success(self):
        # Arrange
        restaurant_id = ObjectId()
        menu_item_id = "item-123"
        
        existing_item = MenuItemModel(
            id=menu_item_id,
            name="Margherita Pizza",
            price=12.99,
            category="Main"
        )
        
        existing_model = RestaurantModel(
            id=restaurant_id,
            name="Pizza Palace",
            cuisine_type="Italian",
            address="123 Main St",
            phone="123-456-7890",
            rating=4.5,
            menu=[existing_item]
        )
        
        updated_model = RestaurantModel(
            id=restaurant_id,
            name="Pizza Palace",
            cuisine_type="Italian",
            address="123 Main St",
            phone="123-456-7890",
            rating=4.5,
            menu=[]
        )
        
        self.mock_repo.get_by_id = AsyncMock(return_value=existing_model)
        self.mock_repo.remove_menu_item = AsyncMock(return_value=updated_model)

        # Act
        response = await self.service.remove_menu_item(str(restaurant_id), menu_item_id)

        # Assert
        self.assertEqual(len(response.menu), 0)
        self.mock_repo.remove_menu_item.assert_called_once_with(restaurant_id, menu_item_id)
