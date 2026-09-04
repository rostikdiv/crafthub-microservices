package com.milhub.product_service.controller;

import com.milhub.product_service.dto.category.CategoryRequestDTO;
import com.milhub.product_service.dto.category.CategoryResponseDTO;
import com.milhub.product_service.entity.Category;
import com.milhub.product_service.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryController categoryController;

    private Category parentCategory;
    private Category childCategory;

    @BeforeEach
    void setUp() {
        parentCategory = Category.builder()
                .id(1L)
                .name("Tactical")
                .description("Tactical gear")
                .subCategories(new ArrayList<>())
                .build();

        childCategory = Category.builder()
                .id(2L)
                .name("Vests")
                .description("Tactical vests")
                .parent(parentCategory)
                .subCategories(new ArrayList<>())
                .build();

        parentCategory.getSubCategories().add(childCategory);
    }

    @Test
    @DisplayName("createCategory: creates and returns new category with status 201")
    void createCategory_WhenValid_ShouldReturnCreated() {
        CategoryRequestDTO request = new CategoryRequestDTO("Helmets", "Tactical helmets", null);
        when(categoryRepository.findByName("Helmets")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(3L);
            return c;
        });

        ResponseEntity<CategoryResponseDTO> response = categoryController.createCategory(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Helmets");
        assertThat(response.getBody().id()).isEqualTo(3L);
    }

    @Test
    @DisplayName("createCategory: throws CONFLICT when category name already exists")
    void createCategory_WhenDuplicateName_ShouldThrowConflict() {
        CategoryRequestDTO request = new CategoryRequestDTO("Tactical", "Desc", null);
        when(categoryRepository.findByName("Tactical")).thenReturn(Optional.of(parentCategory));

        assertThatThrownBy(() -> categoryController.createCategory(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);

        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("createCategory: throws NOT_FOUND when parent category does not exist")
    void createCategory_WhenParentNotFound_ShouldThrowNotFound() {
        CategoryRequestDTO request = new CategoryRequestDTO("Boots", "Desc", 999L);
        when(categoryRepository.findByName("Boots")).thenReturn(Optional.empty());
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryController.createCategory(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("createCategory: successfully sets parent when parentId provided")
    void createCategory_WithValidParentId_ShouldLinkParent() {
        CategoryRequestDTO request = new CategoryRequestDTO("Plates", "Desc", 1L);
        when(categoryRepository.findByName("Plates")).thenReturn(Optional.empty());
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(parentCategory));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(4L);
            return c;
        });

        ResponseEntity<CategoryResponseDTO> response = categoryController.createCategory(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().parentId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getAllCategories: returns hierarchical tree of categories")
    void getAllCategories_ShouldReturnHierarchicalTree() {
        when(categoryRepository.findAll()).thenReturn(List.of(parentCategory, childCategory));

        List<CategoryResponseDTO> results = categoryController.getAllCategories();

        assertThat(results).hasSize(1);
        CategoryResponseDTO root = results.get(0);
        assertThat(root.name()).isEqualTo("Tactical");
        assertThat(root.subCategories()).hasSize(1);
        assertThat(root.subCategories().get(0).name()).isEqualTo("Vests");
    }

    @Test
    @DisplayName("getCategoryById: returns category when found")
    void getCategoryById_WhenFound_ShouldReturnDTO() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(parentCategory));

        CategoryResponseDTO result = categoryController.getCategoryById(1L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Tactical");
        assertThat(result.subCategories()).hasSize(1);
    }

    @Test
    @DisplayName("getCategoryById: throws NOT_FOUND when category does not exist")
    void getCategoryById_WhenNotFound_ShouldThrowNotFound() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryController.getCategoryById(999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }
}
