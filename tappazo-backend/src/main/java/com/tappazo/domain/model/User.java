package com.tappazo.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Entidad de dominio User (Sección 4).
 * El nickname es global y solo se cambia desde el perfil, nunca dentro de una partida.
 */
public class User {
    private final String id;
    private final String googleId;
    private String nickname;
    private final String email;
    private String profileImage;
    private final Instant createdAt;
    private Instant updatedAt;

    public User(String id, String googleId, String nickname, String email, String profileImage, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.googleId = Objects.requireNonNull(googleId, "googleId must not be null");
        this.nickname = Objects.requireNonNull(nickname, "nickname must not be null");
        this.email = Objects.requireNonNull(email, "email must not be null");
        this.profileImage = profileImage;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public void updateNickname(String newNickname) {
        if (newNickname == null || newNickname.isBlank()) {
            throw new IllegalArgumentException("El nickname no puede estar vacío");
        }
        this.nickname = newNickname.trim();
        this.updatedAt = Instant.now();
    }

    public void updateProfileImage(String profileImage) {
        this.profileImage = profileImage;
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getGoogleId() {
        return googleId;
    }

    public String getNickname() {
        return nickname;
    }

    public String getEmail() {
        return email;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
