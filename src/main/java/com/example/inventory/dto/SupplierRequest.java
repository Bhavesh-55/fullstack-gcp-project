package com.example.inventory.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.example.inventory.entity.OrderStatus;

@Schema(description = "Supplier request")
public record SupplierRequest(
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Email @Size(max = 200) String email,
    @Size(max = 30) String phone,
    @NotNull Boolean active
) {}
