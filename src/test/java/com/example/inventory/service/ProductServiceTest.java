package com.example.inventory.service;

import com.example.inventory.dto.ProductRequest;
import com.example.inventory.entity.Category;
import com.example.inventory.entity.Product;
import com.example.inventory.entity.Supplier;
import com.example.inventory.exception.ResourceNotFoundException;
import com.example.inventory.mapper.InventoryMapper;
import com.example.inventory.repository.*;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductServiceTest {
    @Test
    void linksCategoryAndSupplier() {
        ProductRepository products = mock(ProductRepository.class);
        CategoryRepository categories = mock(CategoryRepository.class);
        SupplierRepository suppliers = mock(SupplierRepository.class);
        Category category = new Category(); category.setName("Devices");
        Supplier supplier = new Supplier(); supplier.setName("Global Supply");
        when(categories.findById(1L)).thenReturn(Optional.of(category));
        when(suppliers.findById(2L)).thenReturn(Optional.of(supplier));
        when(products.save(any(Product.class))).thenAnswer(c -> c.getArgument(0));
        ProductService service = new ProductService(products, categories, suppliers,
                mock(OrderItemRepository.class), new InventoryMapper());
        var response = service.create(new ProductRequest("SKU-1", "Keyboard", null,
                new BigDecimal("499.00"), 10, 1L, 2L, true));
        assertEquals("Keyboard", response.name());
        assertEquals("Devices", response.categoryName());
    }

    @Test
    void failsIfCategoryMissing() {
        ProductService service = new ProductService(mock(ProductRepository.class),
                mock(CategoryRepository.class), mock(SupplierRepository.class),
                mock(OrderItemRepository.class), new InventoryMapper());
        assertThrows(ResourceNotFoundException.class,
                () -> service.create(new ProductRequest("SKU-1", "Keyboard", null,
                        new BigDecimal("499.00"), 10, 99L, 2L, true)));
    }
}
