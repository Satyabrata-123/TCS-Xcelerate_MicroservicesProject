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
import com.fdp.orderservice.entity.OrderStatusHistory;
import com.fdp.orderservice.exception.InvalidOrderStateException;
import com.fdp.orderservice.exception.ResourceNotFoundException;
import com.fdp.orderservice.mapper.OrderMapper;
import com.fdp.orderservice.repository.OrderRepository;
import com.fdp.orderservice.service.OrderEventProducer;
import com.fdp.orderservice.service.OrderSagaOrchestrator;
import com.fdp.orderservice.service.OrderService;
import com.fdp.orderservice.dto.event.OrderCreatedEvent;
import com.fdp.orderservice.dto.event.OrderDeliveredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private final OrderEventProducer orderEventProducer;
    private final OrderSagaOrchestrator orderSagaOrchestrator;

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

        recordStatusChange(order, null, OrderStatus.PENDING, "Order placed successfully");

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

        // Publish OrderCreatedEvent
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("ORDER_CREATED")
                .timestamp(LocalDateTime.now())
                .payload(OrderCreatedEvent.Payload.builder()
                        .orderId(savedOrder.getId())
                        .customerId(savedOrder.getCustomerId())
                        .restaurantId(savedOrder.getRestaurantId())
                        .totalAmount(savedOrder.getTotalAmount())
                        .items(savedOrder.getItems().stream()
                                .map(item -> OrderCreatedEvent.OrderItemDto.builder()
                                        .menuItemId(item.getMenuItemId())
                                        .name(item.getName())
                                        .price(item.getPrice())
                                        .quantity(item.getQuantity())
                                        .build())
                                .collect(Collectors.toList()))
                        .build())
                .build();
        orderEventProducer.sendOrderCreatedEvent(event);

        // Execute Saga Orchestrator to call Payment Service and complete order
        Order finalizedOrder = orderSagaOrchestrator.processOrderSaga(savedOrder);

        return orderMapper.toOrderResponse(finalizedOrder);
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

        OrderStatus previousStatus = order.getStatus();
        if (previousStatus != OrderStatus.PENDING) {
            throw new InvalidOrderStateException("Cannot accept order. Order must be in PENDING status, but current status is " + previousStatus);
        }

        order.setStatus(OrderStatus.ACCEPTED);
        recordStatusChange(order, previousStatus, OrderStatus.ACCEPTED, "Order accepted by restaurant");
        Order savedOrder = orderRepository.save(order);
        log.info("Order {} status updated to ACCEPTED", id);
        return orderMapper.toOrderResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse dispatchOrder(UUID id) {
        log.info("Dispatching order: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order with ID " + id + " not found"));

        OrderStatus previousStatus = order.getStatus();
        if (previousStatus != OrderStatus.ACCEPTED) {
            throw new InvalidOrderStateException("Cannot dispatch order. Order must be in ACCEPTED status, but current status is " + previousStatus);
        }

        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        recordStatusChange(order, previousStatus, OrderStatus.OUT_FOR_DELIVERY, "Order dispatched and is out for delivery");
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

        OrderStatus previousStatus = order.getStatus();
        if (previousStatus != OrderStatus.OUT_FOR_DELIVERY) {
            throw new InvalidOrderStateException("Cannot mark order as delivered. Order must be in OUT_FOR_DELIVERY status, but current status is " + previousStatus);
        }

        order.setStatus(OrderStatus.DELIVERED);
        recordStatusChange(order, previousStatus, OrderStatus.DELIVERED, "Order delivered successfully");
        Order savedOrder = orderRepository.save(order);
        log.info("Order {} status updated to DELIVERED", id);

        // Publish OrderDeliveredEvent
        OrderDeliveredEvent event = OrderDeliveredEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("ORDER_DELIVERED")
                .timestamp(LocalDateTime.now())
                .payload(OrderDeliveredEvent.Payload.builder()
                        .orderId(savedOrder.getId())
                        .customerId(savedOrder.getCustomerId())
                        .deliveredAt(LocalDateTime.now())
                        .build())
                .build();
        orderEventProducer.sendOrderDeliveredEvent(event);

        return orderMapper.toOrderResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(UUID id, String reason) {
        log.info("Cancelling order: {} due to: {}", id, reason);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order with ID " + id + " not found"));

        OrderStatus previousStatus = order.getStatus();
        if (previousStatus == OrderStatus.DELIVERED || previousStatus == OrderStatus.OUT_FOR_DELIVERY) {
            throw new InvalidOrderStateException("Cannot cancel order in status: " + previousStatus);
        }
        if (previousStatus == OrderStatus.CANCELLED) {
            throw new InvalidOrderStateException("Order is already cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
        String cancelReason = (reason != null && !reason.trim().isEmpty()) ? reason : "Order cancelled";
        recordStatusChange(order, previousStatus, OrderStatus.CANCELLED, cancelReason);
        Order savedOrder = orderRepository.save(order);
        log.info("Order {} status updated to CANCELLED", id);
        return orderMapper.toOrderResponse(savedOrder);
    }

    private void recordStatusChange(Order order, OrderStatus previousStatus, OrderStatus newStatus, String reason) {
        OrderStatusHistory history = OrderStatusHistory.builder()
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .reason(reason)
                .build();
        order.addStatusHistory(history);
    }
}
