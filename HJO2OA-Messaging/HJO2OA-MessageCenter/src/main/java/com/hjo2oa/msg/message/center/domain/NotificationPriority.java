package com.hjo2oa.msg.message.center.domain;

public enum NotificationPriority {
    NORMAL,
    URGENT,
    CRITICAL;

    public static NotificationPriority fromUrgency(String urgency) {
        if (urgency == null || urgency.isBlank()) {
            return NORMAL;
        }
        return switch (urgency.trim().toUpperCase()) {
            case "CRITICAL" -> CRITICAL;
            case "HIGH", "URGENT" -> URGENT;
            default -> NORMAL;
        };
    }
}
