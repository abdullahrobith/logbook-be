package com.logbook.project;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Class ini wajib ada biar Spring JPA nggak error.
// Anggap aja ini "pintu masuk" ke tabel projects.
@Entity
@Table(name = "projects")
public class Project {
    
    @Id
    private Long id;
    private String name;

    // Getters dan Setters (opsional buat sekarang, tapi biarin aja)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}