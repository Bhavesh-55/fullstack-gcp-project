package com.example.inventory.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.example.inventory.entity.OrderStatus;

@Schema(description = "Order response")
public record OrderResponse(
    Long id, String orderNumber, String customerName, String customerEmail,
    OrderStatus status, BigDecimal totalAmount,
    List<OrderItemResponse> items,
    Instant createdAt, Instant updatedAt
) {}
