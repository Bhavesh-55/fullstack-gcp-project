package com.example.inventory.service;

import com.example.inventory.dto.*;
import com.example.inventory.entity.Supplier;
import com.example.inventory.exception.*;
import com.example.inventory.mapper.InventoryMapper;
import com.example.inventory.repository.*;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SupplierService {
    private final SupplierRepository repository;
    private final ProductRepository products;
    private final InventoryMapper mapper;

    public SupplierService(SupplierRepository repository, ProductRepository products, InventoryMapper mapper) {
        this.repository = repository;
        this.products = products;
        this.mapper = mapper;
    }

    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Supplier email already exists: " + email);
        }
        Supplier supplier = new Supplier();
        apply(supplier, request);
        return mapper.toSupplier(repository.save(supplier));
    }

    public SupplierResponse get(Long id) { return mapper.toSupplier(findEntity(id)); }

    public Page<SupplierResponse> list(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toSupplier);
    }

    @Transactional
    public SupplierResponse update(Long id, SupplierRequest request) {
        Supplier supplier = findEntity(id);
        if (repository.existsByEmailIgnoreCaseAndIdNot(request.email().trim(), id)) {
            throw new DuplicateResourceException("Supplier email already exists: " + request.email());
        }
        apply(supplier, request);
        return mapper.toSupplier(repository.save(supplier));
    }

    @Transactional
    public void delete(Long id) {
        Supplier supplier = findEntity(id);
        if (products.existsBySupplierId(id)) {
            throw new ResourceInUseException("Supplier is linked to products; delete or reassign products first");
        }
        repository.delete(supplier);
    }

    private Supplier findEntity(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Supplier not found: " + id));
    }

    private void apply(Supplier supplier, SupplierRequest request) {
        supplier.setName(request.name().trim());
        supplier.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        supplier.setPhone(request.phone());
        supplier.setActive(request.active());
    }
}
