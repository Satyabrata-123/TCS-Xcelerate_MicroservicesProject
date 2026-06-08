package com.fdp.orderservice.service;

import com.fdp.orderservice.dto.OrderRequest;
import com.fdp.orderservice.dto.OrderResponse;

import java.util.UUID;

public interface OrderService {
    OrderResponse createOrder(OrderRequest request);
    OrderResponse getOrderById(UUID id);
    OrderResponse acceptOrder(UUID id);
    OrderResponse outForDelivery(UUID id);
    OrderResponse deliverOrder(UUID id);
    void cancelOrder(UUID id, String reason);
}
