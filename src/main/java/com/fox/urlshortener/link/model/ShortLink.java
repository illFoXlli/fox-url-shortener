package com.fox.urlshortener.link.model;

import com.fox.urlshortener.auth.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "short_links")
public class ShortLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 8)
    private String code;

    @Column(name = "original_url", nullable = false, length = 2048)
    private String originalUrl;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "click_count", nullable = false)
    private long clickCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    protected ShortLink() {
    }

    public ShortLink(String code, String originalUrl, Instant expiresAt, User user) {
        this.code = code;
        this.originalUrl = originalUrl;
        this.expiresAt = expiresAt;
        this.user = user;
        this.active = true;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public boolean isActive() {
        return active;
    }

    public long getClickCount() {
        return clickCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public User getUser() {
        return user;
    }

    public void update(String originalUrl, Instant expiresAt, Boolean active) {
        if (originalUrl != null) {
            this.originalUrl = originalUrl;
        }
        if (expiresAt != null) {
            this.expiresAt = expiresAt;
        }
        if (active != null) {
            this.active = active;
        }
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void incrementClickCount() {
        clickCount++;
    }
}
