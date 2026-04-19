package com.hjo2oa.msg.message.center.interfaces;

import java.util.List;

public record NotificationBulkReadRequest(List<String> notificationIds) {
}
