package com.example.inventory.repository;

import com.example.inventory.entity.*;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsBySkuIgnoreCase(String sku);
    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);
    boolean existsByCategoryId(Long categoryId);
    boolean existsBySupplierId(Long supplierId);

    @Override
    @EntityGraph(attributePaths = {"category", "supplier"})
    Page<Product> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"category", "supplier"})
    Optional<Product> findById(Long id);

    @EntityGraph(attributePaths = {"category", "supplier"})
    Page<Product> findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(String name, String sku, Pageable pageable);
}
