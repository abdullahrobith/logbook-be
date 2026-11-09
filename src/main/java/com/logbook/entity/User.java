package com.logbook.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "jenis")
    private String jenis;

    @Column(name = "email_atasan")
    private String emailAtasan;

    @Column(name = "is_admin", nullable = false)
    private boolean admin;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getJenis() { return jenis; }
    public String getEmailAtasan() { return emailAtasan; }
    public boolean isAdmin() { return admin; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    public boolean isActive() { return active; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
