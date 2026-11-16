package com.logbook.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "work_statuses")
public class WorkStatus {
    @Id
    private Short id;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private Short sortOrder;

    public Short getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public Short getSortOrder() { return sortOrder; }
}
