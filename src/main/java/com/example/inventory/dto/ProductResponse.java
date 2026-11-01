package com.example.inventory.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.example.inventory.entity.OrderStatus;

@Schema(description = "Product response")
public record ProductResponse(
    Long id, String sku, String name, String description, BigDecimal price,
    int stockQuantity, boolean active,
    Long categoryId, String categoryName, Long supplierId, String supplierName,
    Instant createdAt, Instant updatedAt
) {}
