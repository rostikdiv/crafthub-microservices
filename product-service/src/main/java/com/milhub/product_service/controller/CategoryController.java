package com.milhub.product_service.controller;

import com.milhub.product_service.dto.category.CategoryRequestDTO;
import com.milhub.product_service.dto.category.CategoryResponseDTO;
import com.milhub.product_service.entity.Category;
import com.milhub.product_service.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for managing product categories.
 */
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;

    /**
     * Creates a new product category.
     *
     * @param request the category creation request
     * @return the created category details
     */
    @PreAuthorize("hasAuthority('product:create')")
    @PostMapping("/")
    public ResponseEntity<CategoryResponseDTO> createCategory(@RequestBody CategoryRequestDTO request) {
        // 1. Check for duplicates
        if (categoryRepository.findByName(request.name()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category already exists");
        }

        Category parent = null;
        if (request.parentId() != null) {
            parent = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent category not found"));
        }

        // 2. Map DTO -> Entity
        Category category = Category.builder()
                .name(request.name())
                .description(request.description())
                .parent(parent)
                .build();

        // 3. Save to database
        Category savedCategory = categoryRepository.save(category);

        // 4. Map Entity -> Response DTO
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapToResponse(savedCategory));
    }

    /**
     * Retrieves all available product categories built hierarchically in memory (single SQL query).
     *
     * @return list of root categories with nested subcategories
     */
    @GetMapping("/")
    public List<CategoryResponseDTO> getAllCategories() {
        List<Category> allCategories = categoryRepository.findAll();

        // Group child categories by their parent ID in memory to prevent N+1 lazy queries
        Map<Long, List<Category>> childrenByParentId = allCategories.stream()
                .filter(c -> c.getParent() != null)
                .collect(Collectors.groupingBy(c -> c.getParent().getId()));

        return allCategories.stream()
                .filter(c -> c.getParent() == null) // Root categories
                .map(root -> mapToTreeResponse(root, childrenByParentId))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a specific category by its ID.
     *
     * @param id category ID
     * @return category details
     */
    @GetMapping("/{id}")
    public CategoryResponseDTO getCategoryById(@PathVariable Long id) {
        return (categoryRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found")));
    }

    private CategoryResponseDTO mapToTreeResponse(Category category, Map<Long, List<Category>> childrenByParentId) {
        List<Category> children = childrenByParentId.getOrDefault(category.getId(), Collections.emptyList());
        List<CategoryResponseDTO> subCats = children.isEmpty() ? null : children.stream()
                .map(child -> mapToTreeResponse(child, childrenByParentId))
                .collect(Collectors.toList());

        return CategoryResponseDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .subCategories(subCats)
                .build();
    }

    private CategoryResponseDTO mapToResponse(Category category) {
        List<CategoryResponseDTO> subCats = null;
        if (category.getSubCategories() != null && !category.getSubCategories().isEmpty()) {
            subCats = category.getSubCategories().stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        return CategoryResponseDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .subCategories(subCats)
                .build();
    }
}