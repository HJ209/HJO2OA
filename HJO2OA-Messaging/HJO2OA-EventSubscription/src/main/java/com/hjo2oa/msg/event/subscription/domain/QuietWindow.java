package com.hjo2oa.msg.event.subscription.domain;

import java.time.LocalTime;
import java.util.Objects;

public record QuietWindow(
        LocalTime startsAt,
        LocalTime endsAt
) {

    public QuietWindow {
        Objects.requireNonNull(startsAt, "startsAt must not be null");
        Objects.requireNonNull(endsAt, "endsAt must not be null");
        if (startsAt.equals(endsAt)) {
            throw new IllegalArgumentException("quiet window start and end must be different");
        }
    }

    public boolean contains(LocalTime time) {
        Objects.requireNonNull(time, "time must not be null");
        if (startsAt.isBefore(endsAt)) {
            return !time.isBefore(startsAt) && time.isBefore(endsAt);
        }
        return !time.isBefore(startsAt) || time.isBefore(endsAt);
    }
}
