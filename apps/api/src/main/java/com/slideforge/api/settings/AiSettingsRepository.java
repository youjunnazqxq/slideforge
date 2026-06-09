package com.slideforge.api.settings;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiSettingsRepository extends JpaRepository<AiSettings, UUID> {

    Optional<AiSettings> findByUserId(String userId);
}

