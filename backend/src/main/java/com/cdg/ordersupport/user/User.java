package com.cdg.ordersupport.user;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "users")
public class User {
    @Id private UUID id;
    @Column(nullable = false, unique = true) private String email;
    @Column(name = "password_hash", nullable = false) private String passwordHash;
    @Column(name = "display_name", nullable = false) private String displayName;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Role role;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected User() {}
    public User(String email, String passwordHash, String displayName, Role role) { this.id=UUID.randomUUID(); this.email=email; this.passwordHash=passwordHash; this.displayName=displayName; this.role=role; this.createdAt=Instant.now(); this.updatedAt=this.createdAt; }
    public UUID getId(){return id;} public String getEmail(){return email;} public String getPasswordHash(){return passwordHash;} public String getDisplayName(){return displayName;} public Role getRole(){return role;}
}
