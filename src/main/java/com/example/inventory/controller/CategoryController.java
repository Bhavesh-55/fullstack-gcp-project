package com.example.inventory.controller;

import com.example.inventory.dto.*;
import com.example.inventory.exception.ApiError;
import com.example.inventory.service.CategoryService;
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
@RequestMapping("/api/v1/categories")
@Validated
@Tag(name = "Category", description = "Category management REST endpoints")
public class CategoryController {
    private final CategoryService service;

    public CategoryController(CategoryService service) { this.service = service; }

    @Operation(summary = "Create category")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/categories/" + response.id())).body(response);
    }

    @Operation(summary = "Get category by ID")
    @ApiResponse(responseCode = "200", description = "Found")
    @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @GetMapping("/{id}")
    public CategoryResponse get(@PathVariable @Positive Long id) {
        return service.get(id);
    }

    @Operation(summary = "List categories with pagination")
    @GetMapping
    public Page<CategoryResponse> list(@ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return service.list(pageable);
    }

    @Operation(summary = "Update category")
    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable @Positive Long id, @Valid @RequestBody CategoryRequest request) {
        return service.update(id, request);
    }


    @Operation(summary = "Delete category")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive Long id) {
        service.delete(id);
    }

}
