package com.hjo2oa.biz.collaboration.hub.domain;

import java.util.Map;
import java.util.Set;

public final class CollaborationConstants {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_ARCHIVED = "ARCHIVED";
    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_CLOSED = "CLOSED";
    public static final String STATUS_TODO = "TODO";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_BLOCKED = "BLOCKED";
    public static final String STATUS_DONE = "DONE";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_SCHEDULED = "SCHEDULED";
    public static final String STATUS_ONGOING = "ONGOING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SENT = "SENT";

    public static final String ROLE_OWNER = "OWNER";
    public static final String ROLE_MANAGER = "MANAGER";
    public static final String ROLE_MEMBER = "MEMBER";
    public static final String ROLE_VIEWER = "VIEWER";

    public static final String PERMISSION_READ = "READ";
    public static final String PERMISSION_COMMENT = "COMMENT";
    public static final String PERMISSION_CREATE_DISCUSSION = "CREATE_DISCUSSION";
    public static final String PERMISSION_MANAGE_DISCUSSION = "MANAGE_DISCUSSION";
    public static final String PERMISSION_CREATE_TASK = "CREATE_TASK";
    public static final String PERMISSION_MANAGE_TASK = "MANAGE_TASK";
    public static final String PERMISSION_CREATE_MEETING = "CREATE_MEETING";
    public static final String PERMISSION_MANAGE_MEETING = "MANAGE_MEETING";
    public static final String PERMISSION_MANAGE_MEMBERS = "MANAGE_MEMBERS";

    public static final String PRIORITY_LOW = "LOW";
    public static final String PRIORITY_NORMAL = "NORMAL";
    public static final String PRIORITY_HIGH = "HIGH";
    public static final String PRIORITY_URGENT = "URGENT";

    public static final String OBJECT_DISCUSSION = "DISCUSSION";
    public static final String OBJECT_TASK = "TASK";
    public static final String OBJECT_MEETING = "MEETING";
    public static final String OBJECT_WORKSPACE = "WORKSPACE";

    public static final Map<String, Set<String>> TASK_TRANSITIONS = Map.of(
            STATUS_TODO, Set.of(STATUS_IN_PROGRESS, STATUS_BLOCKED, STATUS_CANCELLED),
            STATUS_IN_PROGRESS, Set.of(STATUS_BLOCKED, STATUS_DONE, STATUS_CANCELLED),
            STATUS_BLOCKED, Set.of(STATUS_IN_PROGRESS, STATUS_CANCELLED),
            STATUS_DONE, Set.of(),
            STATUS_CANCELLED, Set.of()
    );

    public static final Map<String, Set<String>> MEETING_TRANSITIONS = Map.of(
            STATUS_SCHEDULED, Set.of(STATUS_ONGOING, STATUS_COMPLETED, STATUS_CANCELLED),
            STATUS_ONGOING, Set.of(STATUS_COMPLETED, STATUS_CANCELLED),
            STATUS_COMPLETED, Set.of(),
            STATUS_CANCELLED, Set.of()
    );

    private CollaborationConstants() {
    }
}
