package com.cdg.notificationservice.notification;

public class InvalidNotificationEventException extends RuntimeException {
    public InvalidNotificationEventException(String message) { super(message); }
    public InvalidNotificationEventException(String message, Throwable cause) { super(message, cause); }
}
