package com.crafthub.product_service.controller;

import com.crafthub.product_service.dto.category.CategoryRequestDTO;
import com.crafthub.product_service.dto.category.CategoryResponseDTO;
import com.crafthub.product_service.entity.Category;
import com.crafthub.product_service.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
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

        // 2. Map DTO -> Entity
        Category category = Category.builder()
                .name(request.name())
                .description(request.description())
                .build();

        // 3. Save to database
        Category savedCategory = categoryRepository.save(category);

        // 4. Map Entity -> Response DTO
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapToResponse(savedCategory));
    }

    /**
     * Retrieves all available product categories.
     *
     * @return list of categories
     */
    @GetMapping("/")
    public List<CategoryResponseDTO> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::mapToResponse)
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

    // Helper method for mapping
    private CategoryResponseDTO mapToResponse(Category category) {
        return CategoryResponseDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }
}