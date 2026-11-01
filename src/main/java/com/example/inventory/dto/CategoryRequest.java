package com.example.inventory.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.example.inventory.entity.OrderStatus;

@Schema(description = "Category request")
public record CategoryRequest(
    @NotBlank @Size(max = 120) @Schema(example = "Electronics") String name,
    @Size(max = 500) String description,
    @NotNull Boolean active
) {}
