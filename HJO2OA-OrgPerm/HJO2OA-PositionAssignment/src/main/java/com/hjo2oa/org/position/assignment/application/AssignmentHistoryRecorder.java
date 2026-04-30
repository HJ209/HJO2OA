package com.hjo2oa.org.position.assignment.application;

import com.hjo2oa.org.position.assignment.domain.Assignment;
import java.time.Instant;

public interface AssignmentHistoryRecorder {

    AssignmentHistoryRecorder NOOP = (assignment, action, changedAt) -> { };

    void record(Assignment assignment, String action, Instant changedAt);
}
