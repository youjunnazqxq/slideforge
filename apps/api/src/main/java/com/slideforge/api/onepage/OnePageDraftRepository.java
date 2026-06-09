package com.slideforge.api.onepage;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OnePageDraftRepository extends JpaRepository<OnePageDraftEntity, UUID> {
}

