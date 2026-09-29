package com.company.jobmanagement.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Task category system with two tiers:
 * - System Categories: Created by Manager, applied system-wide
 * - Custom Categories: Created by Team Lead, scoped to their team
 */
@Entity
@Table(name = "categories", indexes = {
    @Index(name = "idx_categories_tier", columnList = "tier"),
    @Index(name = "idx_categories_owner_id", columnList = "owner_id"),
    @Index(name = "idx_categories_status", columnList = "status"),
    @Index(name = "idx_categories_code", columnList = "code", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String code;  // uppercase, auto-generated: CONG_VAN, HOP_NOI_BO, etc.

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String icon;  // calendar, document, contract, etc.

    @Column(length = 7)
    private String color;  // hex color: #4A90E2

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CategoryTier tier = CategoryTier.SYSTEM;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;  // null if SYSTEM, set if CUSTOM (the Team Lead who created it)

    @Column(length = 100)
    private String ownerName;  // cached for display

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<String> allowedTaskTypes = new ArrayList<>();  // FAST, OFTEN, MULTI_STEP

    @Column(nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CategoryStatus status = CategoryStatus.ACTIVE;

    @Column
    @Builder.Default
    private Long taskCount = 0L;  // readonly: number of tasks using this category

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ExtraField> extraFields = new ArrayList<>();

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    @Builder.Default
    private ZonedDateTime updatedAt = ZonedDateTime.now();

    @Version
    private Long version;

    public boolean isSystem() {
        return tier == CategoryTier.SYSTEM;
    }

    public boolean isCustom() {
        return tier == CategoryTier.CUSTOM;
    }

    public boolean isActive() {
        return status == CategoryStatus.ACTIVE;
    }

    @Getter
    public enum CategoryTier {
        SYSTEM("Hệ thống"),
        CUSTOM("Tuỳ chỉnh");

        private final String displayName;

        CategoryTier(String displayName) {
            this.displayName = displayName;
        }
    }

    @Getter
    public enum CategoryStatus {
        ACTIVE("Hoạt động"),
        INACTIVE("Vô hiệu hoá");

        private final String displayName;

        CategoryStatus(String displayName) {
            this.displayName = displayName;
        }
    }
}
