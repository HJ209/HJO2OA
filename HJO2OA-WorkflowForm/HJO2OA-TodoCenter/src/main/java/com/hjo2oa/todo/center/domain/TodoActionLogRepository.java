package com.hjo2oa.todo.center.domain;

import java.time.Instant;

public interface TodoActionLogRepository {

    boolean registerIfAbsent(String idempotencyKey, String actionType, String targetId, Instant processedAt);
}
