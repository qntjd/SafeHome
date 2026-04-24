package com.safehome.safehome_api.domain.user.repository;

import com.safehome.safehome_api.domain.user.entity.EmergencyContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, UUID> {
    List<EmergencyContact> findAllByUserId(UUID userId);
}
