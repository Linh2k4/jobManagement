package com.company.jobmanagement.dto.response;

import com.company.jobmanagement.model.entity.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "Category", description = "Task category, system-wide or Lead-owned")
public class CategoryResponse {

    private Long id;
    private String code;
    private String name;
    private String description;
    private String icon;
    private String color;
    private Category.CategoryTier tier;
    private Long ownerId;
    private String ownerName;
    private List<String> allowedTaskTypes;
    private Integer sortOrder;
    private Category.CategoryStatus status;
    private Long taskCount;
    private List<ExtraFieldResponse> extraFields;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
}
