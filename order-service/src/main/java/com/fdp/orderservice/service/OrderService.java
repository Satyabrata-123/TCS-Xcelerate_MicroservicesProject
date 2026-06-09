package com.fdp.orderservice.service;

import com.fdp.orderservice.dto.OrderRequest;
import com.fdp.orderservice.dto.OrderResponse;

import java.util.UUID;

public interface OrderService {
    OrderResponse createOrder(OrderRequest request);
    OrderResponse getOrderById(UUID id);
    OrderResponse acceptOrder(UUID id);
    OrderResponse dispatchOrder(UUID id);
    OrderResponse deliverOrder(UUID id);
    OrderResponse cancelOrder(UUID id, String reason);
}
