package com.fdp.orderservice.controller;

import com.fdp.orderservice.dto.OrderRequest;
import com.fdp.orderservice.dto.OrderResponse;
import com.fdp.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Order API", description = "Endpoints for creating and managing food orders, and updating delivery lifecycle states.")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Create a new food order", description = "Places an order, validates the restaurant and item availability, snapshots current item prices, and calculates the total amount.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order created successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or business rule violation (e.g., unavailable item)",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Restaurant or menu item not found",
                    content = @Content),
            @ApiResponse(responseCode = "502", description = "Communication with external Restaurant Service failed",
                    content = @Content)
    })
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        log.info("REST request to create order for customer: {}", request.getCustomerId());
        OrderResponse response = orderService.createOrder(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order details by ID", description = "Retrieves an order by its unique UUID, including all child order items.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order retrieved successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content)
    })
    public ResponseEntity<OrderResponse> getOrderById(
            @Parameter(description = "Unique identifier of the order", required = true)
            @PathVariable UUID id) {
        log.info("REST request to get order: {}", id);
        OrderResponse response = orderService.getOrderById(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/accept")
    @Operation(summary = "Accept a pending order", description = "Transitions an order's status from PENDING to ACCEPTED.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order status updated to ACCEPTED",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid status transition",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content)
    })
    public ResponseEntity<OrderResponse> acceptOrder(
            @Parameter(description = "Unique identifier of the order", required = true)
            @PathVariable UUID id) {
        log.info("REST request to accept order: {}", id);
        OrderResponse response = orderService.acceptOrder(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/out-for-delivery")
    @Operation(summary = "Mark order as out for delivery", description = "Transitions an order's status from ACCEPTED to OUT_FOR_DELIVERY.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order status updated to OUT_FOR_DELIVERY",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid status transition",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content)
    })
    public ResponseEntity<OrderResponse> outForDelivery(
            @Parameter(description = "Unique identifier of the order", required = true)
            @PathVariable UUID id) {
        log.info("REST request to set order out for delivery: {}", id);
        OrderResponse response = orderService.outForDelivery(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/deliver")
    @Operation(summary = "Mark order as delivered", description = "Transitions an order's status from OUT_FOR_DELIVERY to DELIVERED.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order status updated to DELIVERED",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid status transition",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content)
    })
    public ResponseEntity<OrderResponse> deliverOrder(
            @Parameter(description = "Unique identifier of the order", required = true)
            @PathVariable UUID id) {
        log.info("REST request to deliver order: {}", id);
        OrderResponse response = orderService.deliverOrder(id);
        return ResponseEntity.ok(response);
    }
}
