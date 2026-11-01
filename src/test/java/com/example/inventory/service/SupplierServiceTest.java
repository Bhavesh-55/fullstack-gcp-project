package com.example.inventory.service;

import com.example.inventory.dto.SupplierRequest;
import com.example.inventory.entity.Supplier;
import com.example.inventory.exception.DuplicateResourceException;
import com.example.inventory.mapper.InventoryMapper;
import com.example.inventory.repository.ProductRepository;
import com.example.inventory.repository.SupplierRepository;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SupplierServiceTest {
    @Test
    void normalizesEmail() {
        SupplierRepository repo = mock(SupplierRepository.class);
        when(repo.save(any(Supplier.class))).thenAnswer(c -> c.getArgument(0));
        SupplierService service = new SupplierService(repo, mock(ProductRepository.class), new InventoryMapper());
        var response = service.create(new SupplierRequest("ABC", "  DEMO@EXAMPLE.COM ", null, true));
        assertEquals("demo@example.com", response.email());
    }

    @Test
    void rejectsExistingEmail() {
        SupplierRepository repo = mock(SupplierRepository.class);
        when(repo.existsByEmailIgnoreCase("demo@example.com")).thenReturn(true);
        SupplierService service = new SupplierService(repo, mock(ProductRepository.class), new InventoryMapper());
        assertThrows(DuplicateResourceException.class, () ->
                service.create(new SupplierRequest("ABC", "demo@example.com", null, true)));
    }
}
