package com.example.inventory.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.example.inventory.entity.OrderStatus;

@Schema(description = "Category response")
public record CategoryResponse(
    Long id, String name, String description, boolean active,
    Instant createdAt, Instant updatedAt
) {}
