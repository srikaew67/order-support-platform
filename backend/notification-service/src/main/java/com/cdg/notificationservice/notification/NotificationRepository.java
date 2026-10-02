package com.cdg.notificationservice.notification;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Optional<Notification> findByEventId(UUID eventId);
    boolean existsByEventId(UUID eventId);
    Page<Notification> findByCustomerIdOrderByCreatedAtDescIdDesc(UUID customerId, Pageable pageable);
}
