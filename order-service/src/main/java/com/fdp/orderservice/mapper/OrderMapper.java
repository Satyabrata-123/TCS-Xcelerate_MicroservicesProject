package com.fdp.orderservice.mapper;

import com.fdp.orderservice.dto.OrderResponse;
import com.fdp.orderservice.dto.OrderItemResponse;
import com.fdp.orderservice.entity.Order;
import com.fdp.orderservice.entity.OrderItem;
import com.fdp.orderservice.dto.OrderStatusHistoryResponse;
import com.fdp.orderservice.entity.OrderStatusHistory;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderResponse toOrderResponse(Order order);

    OrderItemResponse toOrderItemResponse(OrderItem orderItem);

    OrderStatusHistoryResponse toOrderStatusHistoryResponse(OrderStatusHistory orderStatusHistory);

    List<OrderResponse> toOrderResponseList(List<Order> orders);
}
