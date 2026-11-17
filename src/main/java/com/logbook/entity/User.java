package com.logbook.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
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


    @JsonProperty("full_name")
    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "jenis")
    private String jenis;

  
    @JsonProperty("email_atasan")
    @Column(name = "email_atasan")
    private String emailAtasan;


    @JsonProperty("is_admin")
    @Column(name = "is_admin", nullable = false)
    private boolean admin;

    @JsonProperty("password_hash")
    @Column(name = "password_hash")
    private String passwordHash;

    @JsonProperty("must_change_password")
    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    @JsonProperty("is_active")
    @Column(name = "is_active", nullable = false)
    private boolean active;

    @JsonProperty("created_at")
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

    public void setId(Long id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setJenis(String jenis) { this.jenis = jenis; }
    public void setEmailAtasan(String emailAtasan) { this.emailAtasan = emailAtasan; }
    public void setAdmin(boolean admin) { this.admin = admin; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setMustChangePassword(boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }
    public void setActive(boolean active) { this.active = active; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}