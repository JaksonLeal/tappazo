package com.tappazo.infrastructure.persistence.repository;

import com.tappazo.infrastructure.persistence.entity.GameEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaGameRepository extends JpaRepository<GameEntity, String> {
    Optional<GameEntity> findByCode(String code);
    boolean existsByCode(String code);
}
