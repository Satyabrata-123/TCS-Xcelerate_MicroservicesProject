from fastapi import APIRouter, Depends, Query, status
from typing import List, Optional
from motor.motor_asyncio import AsyncIOMotorDatabase

from app.database import get_database
from app.repositories.restaurant_repository import RestaurantRepository
from app.services.restaurant_service import RestaurantService
from app.dto.restaurant_dto import RestaurantCreateRequest, RestaurantUpdateRequest, RestaurantResponse
from app.dto.menu_dto import MenuItemCreateRequest

router = APIRouter(prefix="/api/v1/restaurants", tags=["Restaurants"])

def get_restaurant_repository(db: AsyncIOMotorDatabase = Depends(get_database)) -> RestaurantRepository:
    return RestaurantRepository(db)

def get_restaurant_service(repo: RestaurantRepository = Depends(get_restaurant_repository)) -> RestaurantService:
    return RestaurantService(repo)

@router.post("", response_model=RestaurantResponse, status_code=status.HTTP_201_CREATED)
async def create_restaurant(
    request: RestaurantCreateRequest,
    service: RestaurantService = Depends(get_restaurant_service)
):
    return await service.create_restaurant(request)

@router.get("", response_model=List[RestaurantResponse])
async def search_restaurants(
    name: Optional[str] = Query(None, description="Search by name (case-insensitive, partial match)"),
    cuisine_type: Optional[str] = Query(None, description="Search by cuisine type"),
    min_rating: Optional[float] = Query(None, description="Filter by minimum rating", ge=0.0, le=5.0),
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=100),
    service: RestaurantService = Depends(get_restaurant_service)
):
    return await service.search_restaurants(name, cuisine_type, min_rating, skip, limit)

@router.get("/{id}", response_model=RestaurantResponse)
async def get_restaurant(
    id: str,
    service: RestaurantService = Depends(get_restaurant_service)
):
    return await service.get_restaurant(id)

@router.put("/{id}", response_model=RestaurantResponse)
async def update_restaurant(
    id: str,
    request: RestaurantUpdateRequest,
    service: RestaurantService = Depends(get_restaurant_service)
):
    return await service.update_restaurant(id, request)

@router.delete("/{id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_restaurant(
    id: str,
    service: RestaurantService = Depends(get_restaurant_service)
):
    await service.delete_restaurant(id)

@router.post("/{id}/menu", response_model=RestaurantResponse, status_code=status.HTTP_201_CREATED)
async def add_menu_item(
    id: str,
    request: MenuItemCreateRequest,
    service: RestaurantService = Depends(get_restaurant_service)
):
    return await service.add_menu_item(id, request)

@router.delete("/{id}/menu/{menu_item_id}", response_model=RestaurantResponse)
async def remove_menu_item(
    id: str,
    menu_item_id: str,
    service: RestaurantService = Depends(get_restaurant_service)
):
    return await service.remove_menu_item(id, menu_item_id)
