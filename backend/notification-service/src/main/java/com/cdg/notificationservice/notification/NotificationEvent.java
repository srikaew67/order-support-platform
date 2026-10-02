package com.cdg.notificationservice.notification;

import java.util.UUID;

public record NotificationEvent(UUID eventId, String eventType, UUID subjectId,
        UUID customerId, String status, String correlationId) {}
