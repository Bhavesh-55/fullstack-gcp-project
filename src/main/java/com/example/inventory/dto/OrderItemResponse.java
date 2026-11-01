package com.example.inventory.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.example.inventory.entity.OrderStatus;

@Schema(description = "OrderItem response")
public record OrderItemResponse(
    Long productId, String productSku, String productName,
    Integer quantity, BigDecimal unitPrice, BigDecimal lineTotal
) {}
