package com.example.inventory.service;

import com.example.inventory.dto.*;
import com.example.inventory.entity.*;
import com.example.inventory.exception.*;
import com.example.inventory.mapper.InventoryMapper;
import com.example.inventory.repository.*;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderService {
    private final OrderRepository orders;
    private final ProductRepository products;
    private final InventoryMapper mapper;

    public OrderService(OrderRepository orders, ProductRepository products, InventoryMapper mapper) {
        this.orders = orders;
        this.products = products;
        this.mapper = mapper;
    }

    @Transactional
    public OrderResponse create(OrderRequest request) {
        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase(Locale.ROOT));
        order.setStatus(OrderStatus.PENDING);
        apply(order, request);
        return mapper.toOrder(orders.save(order));
    }

    public OrderResponse get(Long id) { return mapper.toOrder(findEntity(id)); }

    public Page<OrderResponse> list(Pageable pageable) {
        return orders.findAll(pageable).map(mapper::toOrder);
    }

    @Transactional
    public OrderResponse update(Long id, OrderRequest request) {
        PurchaseOrder order = findEntity(id);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessRuleException("Only PENDING orders can be edited");
        }
        apply(order, request);
        return mapper.toOrder(orders.save(order));
    }

    @Transactional
    public OrderResponse changeStatus(Long id, OrderStatusRequest request) {
        PurchaseOrder order = findEntity(id);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessRuleException("Only PENDING orders may change status");
        }
        if (request.status() == OrderStatus.PENDING) {
            throw new BusinessRuleException("Choose CONFIRMED or CANCELLED");
        }
        order.setStatus(request.status());
        return mapper.toOrder(orders.save(order));
    }

    @Transactional
    public void delete(Long id) { orders.delete(findEntity(id)); }

    private PurchaseOrder findEntity(Long id) {
        return orders.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Order not found: " + id));
    }

    private void apply(PurchaseOrder order, OrderRequest request) {
        order.setCustomerName(request.customerName().trim());
        order.setCustomerEmail(request.customerEmail().trim().toLowerCase(Locale.ROOT));
        order.clearItems();
        BigDecimal total = BigDecimal.ZERO;
        Set<Long> seenProductIds = new HashSet<>();
        for (OrderItemRequest input : request.items()) {
            if (!seenProductIds.add(input.productId())) {
                throw new BusinessRuleException("Duplicate product in order: " + input.productId());
            }
            Product product = products.findById(input.productId()).orElseThrow(() ->
                    new ResourceNotFoundException("Product not found: " + input.productId()));
            if (!product.getActive()) {
                throw new BusinessRuleException("Product is inactive: " + product.getSku());
            }
            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(input.quantity());
            item.setUnitPrice(product.getPrice());  // Snapshot price at ordering time
            item.setLineTotal(product.getPrice().multiply(BigDecimal.valueOf(input.quantity())));
            total = total.add(item.getLineTotal());
            order.addItem(item);
        }
        order.setTotalAmount(total);
        // This phase intentionally does NOT reserve or decrement Product.stockQuantity.
    }
}
