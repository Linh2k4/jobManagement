package com.company.jobmanagement.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic field schema for task categories.
 * Allows Lead to define custom fields per category (up to 10 fields).
 * Supports 7 field types: TEXT, TEXTAREA, NUMBER, DATE, DATETIME, SELECT, MULTISELECT, CHECKBOX
 */
@Entity
@Table(name = "extra_fields", indexes = {
    @Index(name = "idx_extra_fields_category_id", columnList = "category_id"),
    @Index(name = "idx_extra_fields_field_type", columnList = "field_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtraField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 255)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FieldType fieldType;

    @Column(nullable = false)
    @Builder.Default
    private Boolean required = false;

    @Column(length = 500)
    private String placeholder;

    @Column(columnDefinition = "TEXT")
    private String defaultValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<String> options = new ArrayList<>();  // for SELECT/MULTISELECT

    @Column
    @Builder.Default
    private Integer sortOrder = 0;

    @Column
    @Builder.Default
    private Boolean visibleInList = false;  // show as column in task list

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    @Builder.Default
    private ZonedDateTime updatedAt = ZonedDateTime.now();

    @Version
    private Long version;

    public enum FieldType {
        TEXT("Văn bản 1 dòng"),
        TEXTAREA("Văn bản nhiều dòng"),
        NUMBER("Số"),
        DATE("Ngày"),
        DATETIME("Ngày giờ"),
        SELECT("Chọn 1 giá trị"),
        MULTISELECT("Chọn nhiều giá trị"),
        CHECKBOX("Hộp tích");

        private final String displayName;

        FieldType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
