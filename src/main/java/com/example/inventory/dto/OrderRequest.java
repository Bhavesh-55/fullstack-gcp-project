package com.example.inventory.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.example.inventory.entity.OrderStatus;

@Schema(description = "Order request")
public record OrderRequest(
    @NotBlank @Size(max = 120) String customerName,
    @NotBlank @Email @Size(max = 200) String customerEmail,
    @NotEmpty @Valid List<@Valid OrderItemRequest> items
) {}
