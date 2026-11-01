package com.example.inventory.service;

import com.example.inventory.dto.*;
import com.example.inventory.entity.*;
import com.example.inventory.exception.*;
import com.example.inventory.mapper.InventoryMapper;
import com.example.inventory.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository repository;
    private final CategoryRepository categories;
    private final SupplierRepository suppliers;
    private final OrderItemRepository orderItems;
    private final InventoryMapper mapper;

    public ProductService(ProductRepository repository, CategoryRepository categories,
            SupplierRepository suppliers, OrderItemRepository orderItems, InventoryMapper mapper) {
        this.repository = repository;
        this.categories = categories;
        this.suppliers = suppliers;
        this.orderItems = orderItems;
        this.mapper = mapper;
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        String sku = request.sku().trim();
        if (repository.existsBySkuIgnoreCase(sku)) {
            throw new DuplicateResourceException("Product SKU already exists: " + sku);
        }
        Product product = new Product();
        apply(product, request);
        return mapper.toProduct(repository.save(product));
    }

    public ProductResponse get(Long id) { return mapper.toProduct(findEntity(id)); }

    public Page<ProductResponse> list(String search, Pageable pageable) {
        Page<Product> result = (search == null || search.isBlank())
                ? repository.findAll(pageable)
                : repository.findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(
                        search.trim(), search.trim(), pageable);
        return result.map(mapper::toProduct);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findEntity(id);
        if (repository.existsBySkuIgnoreCaseAndIdNot(request.sku().trim(), id)) {
            throw new DuplicateResourceException("Product SKU already exists: " + request.sku());
        }
        apply(product, request);
        return mapper.toProduct(repository.save(product));
    }

    @Transactional
    public void delete(Long id) {
        Product product = findEntity(id);
        if (orderItems.existsByProductId(id)) {
            throw new ResourceInUseException("Product is referenced by orders; deletion is not permitted");
        }
        repository.delete(product);
    }

    private Product findEntity(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Product not found: " + id));
    }

    private void apply(Product product, ProductRequest request) {
        Category category = categories.findById(request.categoryId()).orElseThrow(() ->
                new ResourceNotFoundException("Category not found: " + request.categoryId()));
        Supplier supplier = suppliers.findById(request.supplierId()).orElseThrow(() ->
                new ResourceNotFoundException("Supplier not found: " + request.supplierId()));
        product.setSku(request.sku().trim());
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setActive(request.active());
        product.setCategory(category);
        product.setSupplier(supplier);
    }
}
