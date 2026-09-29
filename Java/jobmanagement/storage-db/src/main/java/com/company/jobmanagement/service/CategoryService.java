package com.company.jobmanagement.service;

import com.company.jobmanagement.model.entity.Category;
import com.company.jobmanagement.model.entity.ExtraField;
import com.company.jobmanagement.model.entity.User;
import com.company.jobmanagement.model.enums.Role;
import com.company.jobmanagement.exception.BusinessLogicException;
import com.company.jobmanagement.exception.ForbiddenOperationException;
import com.company.jobmanagement.exception.ResourceNotFoundException;
import com.company.jobmanagement.repository.CategoryRepository;
import com.company.jobmanagement.repository.ExtraFieldRepository;
import com.company.jobmanagement.repository.UserRepository;
import com.company.jobmanagement.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Category management service.
 * Handles creation, modification, and deletion of task categories.
 * Enforces permission rules: Manager creates system categories,
 * Team Lead creates custom categories for their team.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ExtraFieldRepository extraFieldRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;
    private static final int MAX_CUSTOM_CATEGORIES_PER_LEAD = 20;
    private static final int MAX_EXTRA_FIELDS_PER_CATEGORY = 10;

    /**
     * Create a new category.
     * Manager can create SYSTEM categories.
     * Team Lead can create CUSTOM categories (scoped to their team).
     */
    public Category createCategory(String name, String description, String icon, String color,
                                   Category.CategoryTier tier, List<String> allowedTaskTypes) {
        User user = currentUser.getCurrentUser();

        // Permission check
        if (tier == Category.CategoryTier.SYSTEM && !user.isManager()) {
            throw new ForbiddenOperationException("Only Manager can create system categories");
        }
        if (tier == Category.CategoryTier.CUSTOM && !user.isLead()) {
            throw new ForbiddenOperationException("Only Team Lead can create custom categories");
        }

        // Validate max custom categories per Lead
        if (tier == Category.CategoryTier.CUSTOM) {
            long count = categoryRepository.countCustomCategoriesForOwner(user.getId());
            if (count >= MAX_CUSTOM_CATEGORIES_PER_LEAD) {
                throw new BusinessLogicException(
                    String.format("Cannot create more than %d custom categories", MAX_CUSTOM_CATEGORIES_PER_LEAD)
                );
            }
        }

        // Scope.md §6.4: a name can't collide with a System category, nor
        // with another category the same owner already has (System names
        // are checked against everyone since they're global).
        Long scopeOwnerId = tier == Category.CategoryTier.CUSTOM ? user.getId() : null;
        if (!isNameAvailable(name, null, scopeOwnerId)) {
            throw new BusinessLogicException("A category with this name already exists in this scope");
        }

        // Generate code from name
        String code = generateCategoryCode(name, tier == Category.CategoryTier.CUSTOM ? user.getId() : null);

        Category category = Category.builder()
                .name(name)
                .code(code)
                .description(description)
                .icon(icon)
                .color(color)
                .tier(tier)
                .owner(tier == Category.CategoryTier.CUSTOM ? user : null)
                .ownerName(tier == Category.CategoryTier.CUSTOM ? user.getFullName() : null)
                .allowedTaskTypes(allowedTaskTypes)
                .status(Category.CategoryStatus.ACTIVE)
                .build();

        Category saved = categoryRepository.save(category);
        log.info("Category created: id={}, code={}, tier={}, owner={}", saved.getId(), saved.getCode(), tier,
                 tier == Category.CategoryTier.CUSTOM ? user.getId() : "SYSTEM");
        return saved;
    }

    /**
     * Get category by ID with eager-loaded extra fields.
     */
    @Transactional(readOnly = true)
    public Category getCategoryById(Long categoryId) {
        return categoryRepository.findByIdWithExtraFields(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
    }

    /**
     * List system categories.
     */
    @Transactional(readOnly = true)
    public List<Category> getSystemCategories() {
        return categoryRepository.findAllSystemCategories();
    }

    /**
     * List custom categories for a Team Lead.
     */
    @Transactional(readOnly = true)
    public List<Category> getCustomCategoriesForLead(Long leadId) {
        return categoryRepository.findCustomCategoriesByOwner(leadId);
    }

    /**
     * List all visible categories for a user.
     * For Team Lead: system + own custom categories.
     * For Member: system + custom of their assigned leads.
     * For Manager: all.
     */
    @Transactional(readOnly = true)
    public List<Category> getVisibleCategoriesForUser() {
        User user = currentUser.getCurrentUser();
        if (user.isManager()) {
            // Manager sees all
            return categoryRepository.findAllActiveWithExtraFields();
        }
        if (user.isLead()) {
            // Team Lead sees system + own custom
            return categoryRepository.findVisibleCategoriesForUser(user.getId());
        }
        // Member sees system + their Lead's custom categories. A Member is
        // never a category owner themselves (only Leads own CUSTOM
        // categories), so querying with the Member's own id here (as this
        // used to) always returned zero custom categories — resolve their
        // Lead's id instead.
        User freshUser = userRepository.findByIdWithLeadAndManager(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + user.getId()));
        if (freshUser.getLead() == null) {
            return categoryRepository.findAllSystemCategories();
        }
        return categoryRepository.findVisibleCategoriesForUser(freshUser.getLead().getId());
    }

    /**
     * Whether `name` is free within its scope — no other ACTIVE System
     * category shares it, and (for a CUSTOM name) no other ACTIVE category
     * owned by `scopeOwnerId` shares it either. Pass the category's own id
     * as `excludeCategoryId` when checking during an update.
     */
    @Transactional(readOnly = true)
    public boolean isNameAvailable(String name, Long excludeCategoryId, Long scopeOwnerId) {
        return categoryRepository.findConflictingName(name, scopeOwnerId, excludeCategoryId).isEmpty();
    }

    /**
     * Update category metadata (name, description, icon, color).
     * Code never changes to maintain referential integrity.
     */
    public Category updateCategory(Long categoryId, String name, String description, String icon, String color,
                                   List<String> allowedTaskTypes, Integer sortOrder) {
        Category category = getCategoryById(categoryId);

        // Permission check
        User user = currentUser.getCurrentUser();
        if (category.isCustom() && !category.getOwner().getId().equals(user.getId()) && !user.isManager()) {
            throw new ForbiddenOperationException("Cannot update category you don't own");
        }
        if (category.isSystem() && !user.isManager()) {
            throw new ForbiddenOperationException("Only Manager can update system categories");
        }

        if (name != null && !name.equals(category.getName())) {
            Long scopeOwnerId = category.isCustom() ? category.getOwner().getId() : null;
            if (!isNameAvailable(name, categoryId, scopeOwnerId)) {
                throw new BusinessLogicException("A category with this name already exists in this scope");
            }
        }

        category.setName(name);
        category.setDescription(description);
        category.setIcon(icon);
        category.setColor(color);
        category.setAllowedTaskTypes(allowedTaskTypes);
        if (sortOrder != null) {
            category.setSortOrder(sortOrder);
        }
        category.setUpdatedAt(ZonedDateTime.now());

        Category updated = categoryRepository.save(category);
        log.info("Category updated: id={}, code={}", updated.getId(), updated.getCode());
        return updated;
    }

    /**
     * Deactivate a category (soft delete).
     * Can only deactivate if there are no tasks using it.
     */
    public void deactivateCategory(Long categoryId) {
        Category category = getCategoryById(categoryId);

        // Permission check
        User user = currentUser.getCurrentUser();
        if (category.isCustom() && !category.getOwner().getId().equals(user.getId()) && !user.isManager()) {
            throw new ForbiddenOperationException("Cannot deactivate category you don't own");
        }

        if (category.getTaskCount() > 0) {
            log.warn("Deactivating category with {} tasks: id={}, code={}", category.getTaskCount(), categoryId, category.getCode());
        }

        category.setStatus(Category.CategoryStatus.INACTIVE);
        category.setUpdatedAt(ZonedDateTime.now());
        categoryRepository.save(category);
        log.info("Category deactivated: id={}, code={}", categoryId, category.getCode());
    }

    /**
     * Delete a category (hard delete).
     * Can only delete if taskCount = 0 and no extra fields have data.
     */
    public void deleteCategory(Long categoryId) {
        Category category = getCategoryById(categoryId);

        // Permission check
        User user = currentUser.getCurrentUser();
        if (category.isCustom() && !category.getOwner().getId().equals(user.getId()) && !user.isManager()) {
            throw new ForbiddenOperationException("Cannot delete category you don't own");
        }

        if (category.getTaskCount() > 0) {
            throw new BusinessLogicException("Cannot delete category with tasks. Deactivate instead.");
        }

        categoryRepository.deleteById(categoryId);
        log.info("Category deleted: id={}, code={}", categoryId, category.getCode());
    }

    /**
     * Add extra field to category.
     */
    public ExtraField addExtraField(Long categoryId, ExtraField field) {
        Category category = getCategoryById(categoryId);

        // Permission check
        User user = currentUser.getCurrentUser();
        if (category.isCustom() && !category.getOwner().getId().equals(user.getId()) && !user.isManager()) {
            throw new ForbiddenOperationException("Cannot modify fields for category you don't own");
        }

        // Max fields check
        if (category.getExtraFields().size() >= MAX_EXTRA_FIELDS_PER_CATEGORY) {
            throw new BusinessLogicException(
                String.format("Cannot add more than %d extra fields per category", MAX_EXTRA_FIELDS_PER_CATEGORY)
            );
        }

        // Scope.md §6.3: a field added to a category that already has tasks
        // can't be required — existing tasks have no value for it yet.
        if (category.getTaskCount() > 0 && Boolean.TRUE.equals(field.getRequired())) {
            field.setRequired(false);
        }

        field.setCategory(category);
        ExtraField saved = extraFieldRepository.save(field);
        category.getExtraFields().add(saved);
        log.info("Extra field added: id={}, label={}, categoryId={}", saved.getId(), saved.getLabel(), categoryId);
        return saved;
    }

    /**
     * Update extra field metadata. Scope.md §6.3: once the owning category
     * has tasks, the field's type can't change (existing data would become
     * invalid under a new type) and it can't be forced required (existing
     * tasks have no value for it).
     */
    public ExtraField updateExtraField(Long fieldId, String label, String placeholder, List<String> options,
                                       Integer sortOrder, Boolean visibleInList,
                                       ExtraField.FieldType fieldType, Boolean required) {
        ExtraField field = extraFieldRepository.findById(fieldId)
                .orElseThrow(() -> new ResourceNotFoundException("Extra field not found"));
        boolean hasData = field.getCategory().getTaskCount() > 0;

        if (label != null) {
            field.setLabel(label);
        }
        if (placeholder != null) {
            field.setPlaceholder(placeholder);
        }
        if (options != null) {
            field.setOptions(options);
        }
        if (sortOrder != null) {
            field.setSortOrder(sortOrder);
        }
        if (visibleInList != null) {
            field.setVisibleInList(visibleInList);
        }
        if (fieldType != null && !fieldType.equals(field.getFieldType())) {
            if (hasData) {
                throw new BusinessLogicException("Cannot change field type — tasks already have data for this field");
            }
            field.setFieldType(fieldType);
        }
        if (required != null) {
            if (hasData && required) {
                throw new BusinessLogicException("Cannot make this field required — existing tasks have no value for it");
            }
            field.setRequired(required);
        }
        field.setUpdatedAt(ZonedDateTime.now());

        ExtraField updated = extraFieldRepository.save(field);
        log.info("Extra field updated: id={}, label={}", updated.getId(), updated.getLabel());
        return updated;
    }

    /**
     * Persist a new sort order for a category's own categories list
     * (System categories reordered by Manager, Custom by their owning Lead).
     */
    public void reorderCategories(List<Long> orderedCategoryIds) {
        User user = currentUser.getCurrentUser();
        List<Category> categories = categoryRepository.findAllById(orderedCategoryIds);

        for (Category category : categories) {
            if (category.isCustom() && !category.getOwner().getId().equals(user.getId()) && !user.isManager()) {
                throw new ForbiddenOperationException("Cannot reorder category you don't own");
            }
            if (category.isSystem() && !user.isManager()) {
                throw new ForbiddenOperationException("Only Manager can reorder system categories");
            }
        }

        for (int i = 0; i < orderedCategoryIds.size(); i++) {
            Long id = orderedCategoryIds.get(i);
            int order = i;
            categories.stream()
                    .filter(c -> c.getId().equals(id))
                    .findFirst()
                    .ifPresent(c -> {
                        c.setSortOrder(order);
                        c.setUpdatedAt(ZonedDateTime.now());
                    });
        }
        categoryRepository.saveAll(categories);
    }

    /**
     * Persist a new sort order for one category's extra fields.
     */
    public void reorderExtraFields(Long categoryId, List<Long> orderedFieldIds) {
        Category category = getCategoryById(categoryId);
        User user = currentUser.getCurrentUser();
        if (category.isCustom() && !category.getOwner().getId().equals(user.getId()) && !user.isManager()) {
            throw new ForbiddenOperationException("Cannot reorder fields for category you don't own");
        }

        List<ExtraField> fields = extraFieldRepository.findAllById(orderedFieldIds);
        for (int i = 0; i < orderedFieldIds.size(); i++) {
            Long id = orderedFieldIds.get(i);
            int order = i;
            fields.stream()
                    .filter(f -> f.getId().equals(id))
                    .findFirst()
                    .ifPresent(f -> {
                        f.setSortOrder(order);
                        f.setUpdatedAt(ZonedDateTime.now());
                    });
        }
        extraFieldRepository.saveAll(fields);
    }

    /**
     * Delete extra field from category.
     * Can only delete if taskCount = 0 (no data exists for this category).
     */
    public void deleteExtraField(Long fieldId) {
        ExtraField field = extraFieldRepository.findById(fieldId)
                .orElseThrow(() -> new ResourceNotFoundException("Extra field not found"));

        Category category = field.getCategory();
        if (category.getTaskCount() > 0) {
            throw new BusinessLogicException(
                "Cannot delete extra field. Tasks exist with this category. Deactivate the field instead."
            );
        }

        extraFieldRepository.deleteById(fieldId);
        log.info("Extra field deleted: id={}, categoryId={}", fieldId, category.getId());
    }

    /**
     * Search categories by keyword (name).
     */
    public List<Category> searchByKeyword(String keyword) {
        return categoryRepository.searchByName("%" + keyword + "%");
    }

    /**
     * Get custom categories for a specific user (typically a lead).
     */
    public List<Category> getCustomCategoriesForUser(Long userId) {
        return categoryRepository.findCustomCategoriesByOwner(userId);
    }

    /**
     * Generate category code from name.
     * Format: UPPERCASE + UNDERSCORES, auto-suffixed if duplicate.
     */
    private String generateCategoryCode(String name, Long ownerId) {
        String baseCode = name
                .toUpperCase()
                .trim()
                .replaceAll("[^A-Z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");

        String code = baseCode;
        int suffix = 1;
        while (categoryRepository.findByCode(code).isPresent()) {
            code = baseCode + "_" + suffix;
            suffix++;
        }
        return code;
    }
}
