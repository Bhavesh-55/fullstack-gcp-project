package com.example.inventory.mapper;

import com.example.inventory.dto.*;
import com.example.inventory.entity.*;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {
    public CategoryResponse toCategory(Category entity) {
        return new CategoryResponse(entity.getId(), entity.getName(), entity.getDescription(),
                entity.getActive(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public SupplierResponse toSupplier(Supplier entity) {
        return new SupplierResponse(entity.getId(), entity.getName(), entity.getEmail(),
                entity.getPhone(), entity.getActive(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public ProductResponse toProduct(Product entity) {
        return new ProductResponse(entity.getId(), entity.getSku(), entity.getName(),
                entity.getDescription(), entity.getPrice(), entity.getStockQuantity(), entity.getActive(),
                entity.getCategory().getId(), entity.getCategory().getName(),
                entity.getSupplier().getId(), entity.getSupplier().getName(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public OrderResponse toOrder(PurchaseOrder order) {
        List<OrderItemResponse> lines = order.getItems().stream()
                .map(item -> new OrderItemResponse(item.getProduct().getId(),
                        item.getProduct().getSku(), item.getProduct().getName(),
                        item.getQuantity(), item.getUnitPrice(), item.getLineTotal()))
                .toList();
        return new OrderResponse(order.getId(), order.getOrderNumber(), order.getCustomerName(),
                order.getCustomerEmail(), order.getStatus(), order.getTotalAmount(), lines,
                order.getCreatedAt(), order.getUpdatedAt());
    }
}
