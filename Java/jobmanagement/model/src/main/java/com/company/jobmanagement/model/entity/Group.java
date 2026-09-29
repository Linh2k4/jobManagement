package com.company.jobmanagement.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A Lead-managed team a Member can belong to (Scope.md §13). Additive to
 * {@link User#getLead()} — not a replacement, so every feature that reads
 * the single Lead FK keeps working unchanged; a member's primary
 * {@link GroupMembership} is the practical stand-in wherever a feature
 * isn't yet group-aware.
 */
@Entity
@Table(name = "groups", indexes = {
    @Index(name = "idx_groups_lead_id", columnList = "lead_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false)
    private User lead;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Never serialized to JSON: this Group entity itself gets returned raw
    // in several API responses (e.g. Evaluation.group), and unlike the
    // scalar/lazy-@ManyToOne fields those call sites JOIN FETCH, nobody
    // expects — or fetches — the reverse membership list along with it.
    // Leaving it un-ignored means any such response 500s the moment this
    // collection isn't already initialized (LazyInitializationException).
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private List<GroupMembership> memberships = new ArrayList<>();

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    @Builder.Default
    private ZonedDateTime updatedAt = ZonedDateTime.now();
}
