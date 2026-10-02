package com.cdg.notificationservice.notification;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;

public record NotificationResponse(UUID id, UUID eventId, String eventType, UUID subjectId,
        String message, Instant createdAt, String correlationId) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getEventId(),
                notification.getEventType(), notification.getSubjectId(), notification.getMessage(),
                notification.getCreatedAt(), notification.getCorrelationId());
    }
    public record PageResponse(List<NotificationResponse> content, int page, int size,
            long totalElements, int totalPages) {
        public static PageResponse from(Page<NotificationResponse> page) {
            return new PageResponse(page.getContent(), page.getNumber(), page.getSize(),
                    page.getTotalElements(), page.getTotalPages());
        }
    }
}
