package com.logbook.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "task_types")
public class TaskType {
    @Id
    private Short id;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private Short sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    public Short getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public Short getSortOrder() { return sortOrder; }
    public boolean isActive() { return isActive; }
}
