package com.example.inventory.service;

import com.example.inventory.dto.CategoryRequest;
import com.example.inventory.entity.Category;
import com.example.inventory.exception.DuplicateResourceException;
import com.example.inventory.mapper.InventoryMapper;
import com.example.inventory.repository.CategoryRepository;
import com.example.inventory.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CategoryServiceTest {
    private CategoryRepository categories;
    private CategoryService service;

    @BeforeEach
    void init() {
        categories = mock(CategoryRepository.class);
        service = new CategoryService(categories, mock(ProductRepository.class), new InventoryMapper());
    }

    @Test
    void createsCategory() {
        when(categories.save(any(Category.class))).thenAnswer(call -> call.getArgument(0));
        var created = service.create(new CategoryRequest(" Electronics ", "Devices", true));
        assertEquals("Electronics", created.name());
        verify(categories).save(any(Category.class));
    }

    @Test
    void rejectsDuplicateName() {
        when(categories.existsByNameIgnoreCase("Electronics")).thenReturn(true);
        assertThrows(DuplicateResourceException.class,
                () -> service.create(new CategoryRequest("Electronics", null, true)));
        verify(categories, never()).save(any());
    }
}
