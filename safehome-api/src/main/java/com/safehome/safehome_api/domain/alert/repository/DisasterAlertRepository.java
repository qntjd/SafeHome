package com.safehome.safehome_api.domain.alert.repository;

import com.safehome.safehome_api.domain.alert.entity.DisasterAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DisasterAlertRepository extends JpaRepository<DisasterAlert, UUID> {

    boolean existsByExternalId(String externalId);

    List<DisasterAlert> findTop20ByOrderByIssuedAtDesc();

    Optional<DisasterAlert> findByExternalId(String externalId);
}