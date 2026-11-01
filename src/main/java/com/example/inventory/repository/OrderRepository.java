package com.example.inventory.repository;

import com.example.inventory.entity.*;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<PurchaseOrder, Long> {
    @Override
    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<PurchaseOrder> findById(Long id);
}
