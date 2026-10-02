package com.cdg.ordersupport.order;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface OrderOutboxRepository extends JpaRepository<OrderOutboxEntry, UUID> {
    Page<OrderOutboxEntry> findByPublishedAtIsNullOrderByOccurredAtAsc(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select event from OrderOutboxEntry event where event.eventId = :id and event.publishedAt is null")
    Optional<OrderOutboxEntry> findPendingForUpdate(UUID id);
}
