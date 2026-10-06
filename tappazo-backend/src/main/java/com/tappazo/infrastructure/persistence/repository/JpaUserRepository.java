package com.tappazo.infrastructure.persistence.repository;

import com.tappazo.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaUserRepository extends JpaRepository<UserEntity, String> {
    Optional<UserEntity> findByGoogleId(String googleId);
    Optional<UserEntity> findByEmail(String email);
    boolean existsByGoogleId(String googleId);
}
