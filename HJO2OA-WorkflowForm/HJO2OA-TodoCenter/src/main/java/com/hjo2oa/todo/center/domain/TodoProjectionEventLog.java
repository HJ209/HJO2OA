package com.hjo2oa.todo.center.domain;

import java.util.UUID;

public interface TodoProjectionEventLog {

    boolean registerIfAbsent(UUID eventId);
}
