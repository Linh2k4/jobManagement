package com.company.jobmanagement.controller.category;

import com.company.jobmanagement.controller.BaseController;
import com.company.jobmanagement.dto.request.CreateCategoryRequest;
import com.company.jobmanagement.dto.response.ApiResponse;
import com.company.jobmanagement.dto.response.CategoryResponse;
import com.company.jobmanagement.dto.response.ExtraFieldResponse;
import com.company.jobmanagement.mapper.CategoryMapper;
import com.company.jobmanagement.mapper.ExtraFieldMapper;
import com.company.jobmanagement.model.entity.Category;
import com.company.jobmanagement.model.entity.ExtraField;
import com.company.jobmanagement.security.CurrentUser;
import com.company.jobmanagement.service.CategoryService;
import com.company.jobmanagement.model.entity.ExtraField.FieldType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Task category management with custom fields")
public class CategoryController extends BaseController {

    private final CategoryService categoryService;
    private final CategoryMapper categoryMapper;
    private final ExtraFieldMapper extraFieldMapper;
    private final CurrentUser currentUser;

    @PostMapping
    @Operation(summary = "Create new category")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @RequestBody CreateCategoryRequest request) {
        Category category = categoryService.createCategory(
                request.getName(),
                request.getDescription(),
                request.getIcon(),
                request.getColor(),
                request.getTier(),
                request.getAllowedTaskTypes()
        );
        return created(ApiResponse.success("Category created", categoryMapper.toDTO(category)));
    }

    @GetMapping
    @Operation(summary = "List visible categories (system + user's custom)")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getVisibleCategories() {
        List<CategoryResponse> categories = categoryService.getVisibleCategoriesForUser().stream()
                .map(categoryMapper::toDTO)
                .collect(Collectors.toList());
        return ok(ApiResponse.success(categories));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Long id) {
        Category category = categoryService.getCategoryById(id);
        return ok(ApiResponse.success(categoryMapper.toDTO(category)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update category (name, description, icon, color)")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @RequestBody CreateCategoryRequest request) {
        Category category = categoryService.updateCategory(
                id,
                request.getName(),
                request.getDescription(),
                request.getIcon(),
                request.getColor(),
                request.getAllowedTaskTypes(),
                null
        );
        return ok(ApiResponse.success("Category updated", categoryMapper.toDTO(category)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Deactivate category (soft delete)")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<?>> deactivateCategory(@PathVariable Long id) {
        categoryService.deactivateCategory(id);
        return ok(ApiResponse.success("Category deactivated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Hard delete category (Manager only)")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<?>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ok(ApiResponse.success("Category deleted"));
    }

    @GetMapping("/system")
    @Operation(summary = "List all system categories")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getSystemCategories() {
        List<CategoryResponse> categories = categoryService.getSystemCategories().stream()
                .map(categoryMapper::toDTO)
                .collect(Collectors.toList());
        return ok(ApiResponse.success(categories));
    }

    @GetMapping("/search")
    @Operation(summary = "Search categories by keyword")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> searchCategories(
            @RequestParam String keyword) {
        List<CategoryResponse> categories = categoryService.searchByKeyword(keyword).stream()
                .map(categoryMapper::toDTO)
                .collect(Collectors.toList());
        return ok(ApiResponse.success(categories));
    }

    @PostMapping("/{id}/fields")
    @Operation(summary = "Add extra field to category")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<ExtraFieldResponse>> addExtraField(
            @PathVariable Long id,
            @RequestBody AddExtraFieldRequest request) {
        ExtraField field = ExtraField.builder()
                .label(request.getLabel())
                .fieldType(FieldType.valueOf(request.getFieldType().toUpperCase()))
                .required(request.getRequired())
                .placeholder(request.getPlaceholder())
                .defaultValue(request.getDefaultValue())
                .options(request.getOptions())
                .sortOrder(request.getSortOrder())
                .visibleInList(request.getVisibleInList())
                .build();
        ExtraField saved = categoryService.addExtraField(id, field);
        return created(ApiResponse.success("Extra field added", extraFieldMapper.toDTO(saved)));
    }

    @PutMapping("/{id}/fields/{fieldId}")
    @Operation(summary = "Update extra field")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<ExtraFieldResponse>> updateExtraField(
            @PathVariable Long id,
            @PathVariable Long fieldId,
            @RequestBody AddExtraFieldRequest request) {
        ExtraField updated = categoryService.updateExtraField(
                fieldId,
                request.getLabel(),
                request.getPlaceholder(),
                request.getOptions(),
                request.getSortOrder(),
                request.getVisibleInList(),
                request.getFieldType() != null ? FieldType.valueOf(request.getFieldType().toUpperCase()) : null,
                request.getRequired()
        );
        return ok(ApiResponse.success("Extra field updated", extraFieldMapper.toDTO(updated)));
    }

    @PutMapping("/reorder")
    @Operation(summary = "Persist a new display order for a set of categories")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<?>> reorderCategories(@RequestBody ReorderRequest request) {
        categoryService.reorderCategories(request.getIds());
        return ok(ApiResponse.success("Categories reordered"));
    }

    @PutMapping("/{id}/fields/reorder")
    @Operation(summary = "Persist a new display order for a category's extra fields")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<?>> reorderExtraFields(
            @PathVariable Long id,
            @RequestBody ReorderRequest request) {
        categoryService.reorderExtraFields(id, request.getIds());
        return ok(ApiResponse.success("Fields reordered"));
    }

    @GetMapping("/check-name")
    @Operation(summary = "Check whether a category name is free within its scope")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkNameAvailable(
            @RequestParam String name,
            @RequestParam(required = false) Category.CategoryTier tier,
            @RequestParam(required = false) Long excludeCategoryId) {
        Long scopeOwnerId = tier == Category.CategoryTier.CUSTOM ? currentUser.getCurrentUserId() : null;
        boolean available = categoryService.isNameAvailable(name, excludeCategoryId, scopeOwnerId);
        return ok(ApiResponse.success(Map.of("available", available)));
    }

    @DeleteMapping("/{id}/fields/{fieldId}")
    @Operation(summary = "Delete extra field from category")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<?>> deleteExtraField(
            @PathVariable Long id,
            @PathVariable Long fieldId) {
        categoryService.deleteExtraField(fieldId);
        return ok(ApiResponse.success("Extra field deleted"));
    }

    @GetMapping("/{id}/fields")
    @Operation(summary = "List extra fields for category")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<List<ExtraFieldResponse>>> getExtraFields(@PathVariable Long id) {
        Category category = categoryService.getCategoryById(id);
        List<ExtraFieldResponse> fields = category.getExtraFields().stream()
                .map(extraFieldMapper::toDTO)
                .collect(Collectors.toList());
        return ok(ApiResponse.success(fields));
    }

    @GetMapping("/custom")
    @Operation(summary = "List user's custom categories")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCustomCategories() {
        Long userId = currentUser.getCurrentUserId();
        List<CategoryResponse> categories = (userId != null ? categoryService.getCustomCategoriesForUser(userId) : List.<Category>of())
                .stream()
                .map(categoryMapper::toDTO)
                .collect(Collectors.toList());
        return ok(ApiResponse.success(categories));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get category statistics")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<?>> getCategoryStats() {
        long systemCategories = categoryService.getSystemCategories().size();
        long customCategories = categoryService.getCustomCategoriesForUser(
                currentUser.getCurrentUserId()).size();

        Map<String, Long> stats = Map.of(
                "systemCategories", systemCategories,
                "customCategories", customCategories,
                "total", systemCategories + customCategories
        );
        return ok(ApiResponse.success(stats));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddExtraFieldRequest {
        private String label;
        private String fieldType; // TEXT, TEXTAREA, NUMBER, DATE, DATETIME, SELECT, MULTISELECT, CHECKBOX
        private Boolean required;
        private String placeholder;
        private String defaultValue;
        private List<String> options;
        private Integer sortOrder;
        private Boolean visibleInList;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReorderRequest {
        private List<Long> ids; // in the new display order
    }
}
