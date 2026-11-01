package com.example.inventory.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.example.inventory.entity.OrderStatus;

@Schema(description = "Product request")
public record ProductRequest(
    @NotBlank @Size(max = 60) String sku,
    @NotBlank @Size(max = 160) String name,
    @Size(max = 1000) String description,
    @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
    @NotNull @Min(0) Integer stockQuantity,
    @NotNull @Positive Long categoryId,
    @NotNull @Positive Long supplierId,
    @NotNull Boolean active
) {}
