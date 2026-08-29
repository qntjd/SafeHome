package com.safehome.safehome_api.domain.ai.repository;

import com.safehome.safehome_api.domain.ai.entity.AiBriefing;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface AiBriefingRepository extends JpaRepository<AiBriefing, UUID> {
    Optional<AiBriefing> findByUserIdAndBriefingDate(UUID userId, LocalDate briefingDate);
}