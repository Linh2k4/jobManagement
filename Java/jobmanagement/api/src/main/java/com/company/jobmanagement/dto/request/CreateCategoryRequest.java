package com.company.jobmanagement.dto.request;

import com.company.jobmanagement.model.entity.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "CreateCategoryRequest", description = "Request to create a category")
public class CreateCategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(min = 3, max = 255, message = "Name must be 3-255 characters")
    @Schema(description = "Category name", example = "Họp nội bộ")
    private String name;

    @Schema(description = "Category description", example = "Các buổi họp định kỳ trong nhóm")
    private String description;

    @Schema(description = "Icon name", example = "calendar")
    private String icon;

    @Schema(description = "Hex color code", example = "#4A90E2")
    private String color;

    @Schema(description = "Category tier (SYSTEM or CUSTOM)", example = "CUSTOM", allowableValues = {"SYSTEM", "CUSTOM"})
    private Category.CategoryTier tier;

    @Schema(description = "Allowed task types", example = "[\"FAST\", \"OFTEN\"]")
    private List<String> allowedTaskTypes;
}
