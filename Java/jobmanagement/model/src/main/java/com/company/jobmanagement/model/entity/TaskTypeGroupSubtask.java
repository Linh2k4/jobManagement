package com.company.jobmanagement.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "task_type_group_subtasks", indexes = {
    @Index(name = "idx_task_type_group_subtasks_task_type_id", columnList = "task_type_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskTypeGroupSubtask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_type_id", nullable = false)
    private TaskType taskType;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private Integer groupOrder;

    @OneToMany(mappedBy = "taskTypeGroupSubtask", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TaskTypeSubtask> subtasks = new ArrayList<>();

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();
}
