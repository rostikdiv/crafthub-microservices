package com.crafthub.product_service.controller;

import com.crafthub.product_service.dto.CategoryRequestDTO;
import com.crafthub.product_service.dto.CategoryResponseDTO;
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

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;

    @PreAuthorize("hasAuthority('product:create')")
    @PostMapping("/")
    public ResponseEntity<CategoryResponseDTO> createCategory(@RequestBody CategoryRequestDTO request) {
        // 1. Перевірка на дублікат
        if (categoryRepository.findByName(request.name()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category already exists");
        }

        // 2. Мапінг DTO -> Entity
        Category category = Category.builder()
                .name(request.name())
                .description(request.description())
                .build();

        // 3. Збереження
        Category savedCategory = categoryRepository.save(category);

        // 4. Мапінг Entity -> ResponseDTO
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapToResponse(savedCategory));
    }

    @GetMapping("/")
    public List<CategoryResponseDTO> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public CategoryResponseDTO getCategoryById(@PathVariable Long id) {
        return ( categoryRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found")));
    }

    // Допоміжний метод мапінгу
    private CategoryResponseDTO mapToResponse(Category category) {
        return CategoryResponseDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }
}