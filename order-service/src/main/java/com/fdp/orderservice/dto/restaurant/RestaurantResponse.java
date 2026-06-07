package com.fdp.orderservice.dto.restaurant;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantResponse {
    private String id;
    private String name;
    private String description;

    @JsonProperty("cuisine_type")
    private String cuisineType;

    private String address;
    private String phone;
    private Double rating;
    private List<MenuItemResponse> menu;
}
