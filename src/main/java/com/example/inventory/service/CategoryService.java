package com.example.inventory.service;

import com.example.inventory.dto.*;
import com.example.inventory.entity.Category;
import com.example.inventory.exception.*;
import com.example.inventory.mapper.InventoryMapper;
import com.example.inventory.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository repository;
    private final ProductRepository products;
    private final InventoryMapper mapper;

    public CategoryService(CategoryRepository repository, ProductRepository products, InventoryMapper mapper) {
        this.repository = repository;
        this.products = products;
        this.mapper = mapper;
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        if (repository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Category already exists: " + name);
        }
        Category category = new Category();
        apply(category, request);
        return mapper.toCategory(repository.save(category));
    }

    public CategoryResponse get(Long id) { return mapper.toCategory(findEntity(id)); }

    public Page<CategoryResponse> list(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toCategory);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findEntity(id);
        if (repository.existsByNameIgnoreCaseAndIdNot(request.name().trim(), id)) {
            throw new DuplicateResourceException("Category already exists: " + request.name());
        }
        apply(category, request);
        return mapper.toCategory(repository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        Category category = findEntity(id);
        if (products.existsByCategoryId(id)) {
            throw new ResourceInUseException("Category is linked to products; delete or reassign products first");
        }
        repository.delete(category);
    }

    private Category findEntity(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Category not found: " + id));
    }

    private void apply(Category category, CategoryRequest request) {
        category.setName(request.name().trim());
        category.setDescription(request.description());
        category.setActive(request.active());
    }
}
