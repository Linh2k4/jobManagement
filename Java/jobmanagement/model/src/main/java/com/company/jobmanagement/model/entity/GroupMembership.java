package com.company.jobmanagement.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

/**
 * N-N Member&lt;-&gt;Group link. {@code isPrimary} marks the one group that
 * stands in for the old single-Lead relationship wherever a feature isn't
 * yet group-aware (Scope.md §13.1).
 */
@Entity
@Table(name = "group_memberships", indexes = {
    @Index(name = "idx_group_memberships_group_id", columnList = "group_id"),
    @Index(name = "idx_group_memberships_member_id", columnList = "member_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private User member;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    @Builder.Default
    private ZonedDateTime joinedAt = ZonedDateTime.now();
}
