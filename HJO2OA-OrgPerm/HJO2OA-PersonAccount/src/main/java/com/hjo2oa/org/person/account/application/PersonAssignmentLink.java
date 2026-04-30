package com.hjo2oa.org.person.account.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public interface PersonAssignmentLink {

    PersonAssignmentLink NOOP = (tenantId, personId, endDate, changedAt) -> 0;

    int closeActiveAssignments(UUID tenantId, UUID personId, LocalDate endDate, Instant changedAt);
}
