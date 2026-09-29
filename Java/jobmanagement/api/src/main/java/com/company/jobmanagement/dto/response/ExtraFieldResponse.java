package com.company.jobmanagement.dto.response;

import com.company.jobmanagement.model.entity.ExtraField;
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
@Schema(name = "ExtraField", description = "Custom field defined on a category")
public class ExtraFieldResponse {

    private Long id;
    private String label;
    private ExtraField.FieldType fieldType;
    private Boolean required;
    private String placeholder;
    private String defaultValue;
    private List<String> options;
    private Integer sortOrder;
    private Boolean visibleInList;
    private ZonedDateTime createdAt;
}
