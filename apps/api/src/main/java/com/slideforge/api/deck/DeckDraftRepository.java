package com.slideforge.api.deck;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeckDraftRepository extends JpaRepository<DeckDraftEntity, UUID> {
}
