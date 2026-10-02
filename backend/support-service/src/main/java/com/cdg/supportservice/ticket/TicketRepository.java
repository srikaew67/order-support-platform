package com.cdg.supportservice.ticket;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface TicketRepository extends JpaRepository<SupportTicket, UUID> {
    Page<SupportTicket> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);
    Page<SupportTicket> findAllByOrderByCreatedAtDesc(Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ticket from SupportTicket ticket where ticket.id = :id")
    Optional<SupportTicket> findForUpdate(UUID id);
}
