package com.fdp.orderservice.service.impl;

import com.fdp.orderservice.client.RestaurantClient;
import com.fdp.orderservice.dto.OrderItemRequest;
import com.fdp.orderservice.dto.OrderRequest;
import com.fdp.orderservice.dto.OrderResponse;
import com.fdp.orderservice.dto.restaurant.MenuItemResponse;
import com.fdp.orderservice.dto.restaurant.RestaurantResponse;
import com.fdp.orderservice.entity.Order;
import com.fdp.orderservice.entity.OrderItem;
import com.fdp.orderservice.entity.OrderStatus;
import com.fdp.orderservice.exception.InvalidOrderStateException;
import com.fdp.orderservice.exception.ResourceNotFoundException;
import com.fdp.orderservice.mapper.OrderMapper;
import com.fdp.orderservice.repository.OrderRepository;
import com.fdp.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final RestaurantClient restaurantClient;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        log.info("Creating order for customer: {} at restaurant: {}", request.getCustomerId(), request.getRestaurantId());

        // Fetch restaurant details from restaurant-service to validate existence and get menus
        RestaurantResponse restaurant = restaurantClient.getRestaurantById(request.getRestaurantId());
        if (restaurant == null) {
            throw new ResourceNotFoundException("Restaurant with ID " + request.getRestaurantId() + " not found");
        }

        if (restaurant.getMenu() == null || restaurant.getMenu().isEmpty()) {
            throw new InvalidOrderStateException("Restaurant with ID " + request.getRestaurantId() + " has no menu items configured");
        }

        // Map menu items by their ID for O(1) lookup
        Map<String, MenuItemResponse> menuMap = restaurant.getMenu().stream()
                .collect(Collectors.toMap(MenuItemResponse::getId, Function.identity()));

        Order order = Order.builder()
                .customerId(request.getCustomerId())
                .restaurantId(request.getRestaurantId())
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        // Process and validate each ordered item
        for (OrderItemRequest itemRequest : request.getItems()) {
            MenuItemResponse menuItem = menuMap.get(itemRequest.getMenuItemId());
            if (menuItem == null) {
                throw new ResourceNotFoundException("Menu item with ID " + itemRequest.getMenuItemId() + " not found in restaurant menu");
            }

            if (!menuItem.isAvailable()) {
                throw new InvalidOrderStateException("Menu item '" + menuItem.getName() + "' is currently unavailable");
            }

            BigDecimal itemPrice = menuItem.getPrice();
            BigDecimal itemTotal = itemPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            OrderItem orderItem = OrderItem.builder()
                    .menuItemId(itemRequest.getMenuItemId())
                    .name(menuItem.getName())
                    .price(itemPrice)
                    .quantity(itemRequest.getQuantity())
                    .build();

            order.addItem(orderItem);
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);
        
        log.info("Order created successfully with ID: {} and total amount: {}", savedOrder.getId(), savedOrder.getTotalAmount());
        return orderMapper.toOrderResponse(savedOrder);
    }

    @Override
    public OrderResponse getOrderById(UUID id) {
        log.info("Fetching order by ID: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order with ID " + id + " not found"));
        return orderMapper.toOrderResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse acceptOrder(UUID id) {
        log.info("Accepting order: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order with ID " + id + " not found"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException("Cannot accept order. Order must be in PENDING status, but current status is " + order.getStatus());
        }

        order.setStatus(OrderStatus.ACCEPTED);
        Order savedOrder = orderRepository.save(order);
        log.info("Order {} status updated to ACCEPTED", id);
        return orderMapper.toOrderResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse outForDelivery(UUID id) {
        log.info("Setting order out for delivery: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order with ID " + id + " not found"));

        if (order.getStatus() != OrderStatus.ACCEPTED) {
            throw new InvalidOrderStateException("Cannot set order out for delivery. Order must be in ACCEPTED status, but current status is " + order.getStatus());
        }

        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        Order savedOrder = orderRepository.save(order);
        log.info("Order {} status updated to OUT_FOR_DELIVERY", id);
        return orderMapper.toOrderResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse deliverOrder(UUID id) {
        log.info("Marking order as delivered: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order with ID " + id + " not found"));

        if (order.getStatus() != OrderStatus.OUT_FOR_DELIVERY) {
            throw new InvalidOrderStateException("Cannot mark order as delivered. Order must be in OUT_FOR_DELIVERY status, but current status is " + order.getStatus());
        }

        order.setStatus(OrderStatus.DELIVERED);
        Order savedOrder = orderRepository.save(order);
        log.info("Order {} status updated to DELIVERED", id);
        return orderMapper.toOrderResponse(savedOrder);
    }
}
