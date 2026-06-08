from pydantic import BaseModel
from uuid import UUID
from typing import List
from datetime import datetime
from decimal import Decimal

class OrderItemDto(BaseModel):
    menuItemId: str
    name: str
    price: Decimal
    quantity: int

class OrderCreatedPayload(BaseModel):
    orderId: UUID
    customerId: UUID
    restaurantId: str
    totalAmount: Decimal
    items: List[OrderItemDto]

class PaymentSuccessPayload(BaseModel):
    paymentId: UUID
    orderId: UUID
    amount: Decimal
    currency: str
    paymentMethod: str
    idempotencyKey: str

class PaymentFailedPayload(BaseModel):
    orderId: UUID
    amount: Decimal
    currency: str
    paymentMethod: str
    idempotencyKey: str
    errorMessage: str

class OrderDeliveredPayload(BaseModel):
    orderId: UUID
    customerId: UUID
    deliveredAt: datetime

class BaseEvent(BaseModel):
    eventId: UUID
    eventType: str
    timestamp: datetime

class OrderCreatedEvent(BaseEvent):
    payload: OrderCreatedPayload

class PaymentSuccessEvent(BaseEvent):
    payload: PaymentSuccessPayload

class PaymentFailedEvent(BaseEvent):
    payload: PaymentFailedPayload

class OrderDeliveredEvent(BaseEvent):
    payload: OrderDeliveredPayload
