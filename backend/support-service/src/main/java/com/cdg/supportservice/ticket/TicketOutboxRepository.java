package com.cdg.supportservice.ticket;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface TicketOutboxRepository extends JpaRepository<TicketOutboxEntry, UUID> {
    Page<TicketOutboxEntry> findByPublishedAtIsNullOrderByOccurredAtAsc(Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select event from TicketOutboxEntry event where event.eventId = :id and event.publishedAt is null")
    Optional<TicketOutboxEntry> findPendingForUpdate(UUID id);
}
