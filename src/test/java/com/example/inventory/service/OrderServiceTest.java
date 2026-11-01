package com.example.inventory.service;

import com.example.inventory.dto.*;
import com.example.inventory.entity.*;
import com.example.inventory.exception.BusinessRuleException;
import com.example.inventory.mapper.InventoryMapper;
import com.example.inventory.repository.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceTest {
    @Test
    void usesServerSidePriceToCalculateTotal() {
        OrderRepository orders = mock(OrderRepository.class);
        ProductRepository products = mock(ProductRepository.class);
        Product product = new Product();
        product.setSku("M-1"); product.setName("Mouse");
        product.setPrice(new BigDecimal("12.25")); product.setActive(true);
        when(products.findById(5L)).thenReturn(Optional.of(product));
        when(orders.save(any(PurchaseOrder.class))).thenAnswer(c -> c.getArgument(0));
        OrderService service = new OrderService(orders, products, new InventoryMapper());
        var result = service.create(new OrderRequest("Ada", "ada@example.com",
                List.of(new OrderItemRequest(5L, 2))));
        assertEquals(0, new BigDecimal("24.50").compareTo(result.totalAmount()));
        assertEquals(OrderStatus.PENDING, result.status());
        assertEquals(2, result.items().get(0).quantity());
    }

    @Test
    void rejectsDuplicateProductsWithinOrder() {
        ProductRepository products = mock(ProductRepository.class);
        Product product = new Product();
        product.setSku("M-9"); product.setName("Mouse");
        product.setPrice(new BigDecimal("10.00")); product.setActive(true);
        when(products.findById(9L)).thenReturn(Optional.of(product));
        OrderService service = new OrderService(mock(OrderRepository.class),
                products, new InventoryMapper());
        assertThrows(BusinessRuleException.class, () ->
                service.create(new OrderRequest("Ada", "ada@example.com", List.of(
                        new OrderItemRequest(9L, 1), new OrderItemRequest(9L, 2)))));
    }
}
