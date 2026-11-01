package com.example.inventory.controller;

import com.example.inventory.dto.*;
import com.example.inventory.exception.ApiError;
import com.example.inventory.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/suppliers")
@Validated
@Tag(name = "Supplier", description = "Supplier management REST endpoints")
public class SupplierController {
    private final SupplierService service;

    public SupplierController(SupplierService service) { this.service = service; }

    @Operation(summary = "Create supplier")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @PostMapping
    public ResponseEntity<SupplierResponse> create(@Valid @RequestBody SupplierRequest request) {
        SupplierResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/suppliers/" + response.id())).body(response);
    }

    @Operation(summary = "Get supplier by ID")
    @ApiResponse(responseCode = "200", description = "Found")
    @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @GetMapping("/{id}")
    public SupplierResponse get(@PathVariable @Positive Long id) {
        return service.get(id);
    }

    @Operation(summary = "List suppliers with pagination")
    @GetMapping
    public Page<SupplierResponse> list(@ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return service.list(pageable);
    }

    @Operation(summary = "Update supplier")
    @PutMapping("/{id}")
    public SupplierResponse update(@PathVariable @Positive Long id, @Valid @RequestBody SupplierRequest request) {
        return service.update(id, request);
    }


    @Operation(summary = "Delete supplier")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive Long id) {
        service.delete(id);
    }

}
