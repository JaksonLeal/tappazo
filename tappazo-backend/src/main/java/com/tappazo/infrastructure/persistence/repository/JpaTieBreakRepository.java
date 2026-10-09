package com.tappazo.infrastructure.persistence.repository;

import com.tappazo.infrastructure.persistence.entity.TieBreakEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaTieBreakRepository extends JpaRepository<TieBreakEntity, String> {
    Optional<TieBreakEntity> findBySourceRoundId(String sourceRoundId);
}
