package com.safehome.safehome_api.domain.sos.repository;

import com.safehome.safehome_api.domain.sos.entity.SosLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SosLogRepository extends JpaRepository<SosLog, UUID> {
    List<SosLog> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

}
