package com.fdp.orderservice.client;

import com.fdp.orderservice.dto.restaurant.RestaurantResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "RESTAURANT-SERVICE", path = "/api/v1/restaurants")
public interface RestaurantClient {

    @GetMapping("/{id}")
    RestaurantResponse getRestaurantById(@PathVariable("id") String id);
}
