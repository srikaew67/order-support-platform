package com.cdg.notificationservice.notification;

import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationRepository notifications;
    public NotificationController(NotificationRepository notifications) { this.notifications = notifications; }
    @GetMapping
    public NotificationResponse.PageResponse list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, Authentication auth) {
        if (page < 0 || size < 1 || size > 100)
            throw new NotificationException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid page or size");
        return NotificationResponse.PageResponse.from(notifications
                .findByCustomerIdOrderByCreatedAtDescIdDesc(UUID.fromString(auth.getName()), PageRequest.of(page, size))
                .map(NotificationResponse::from));
    }
    @GetMapping("/{id}")
    public NotificationResponse get(@PathVariable UUID id, Authentication auth) {
        Notification notification = notifications.findById(id).orElseThrow(() -> missing());
        if (!notification.getCustomerId().equals(UUID.fromString(auth.getName())))
            throw new NotificationException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied");
        return NotificationResponse.from(notification);
    }
    private static NotificationException missing() {
        return new NotificationException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Notification not found");
    }
}
