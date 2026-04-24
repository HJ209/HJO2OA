package com.hjo2oa.data.common.domain.model;

import java.util.Objects;
import java.util.UUID;

public abstract class AbstractDomainEntity {

    private final UUID id;

    protected AbstractDomainEntity(UUID id) {
        this.id = Objects.requireNonNull(id, "id must not be null");
    }

    public UUID id() {
        return id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), id);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        AbstractDomainEntity that = (AbstractDomainEntity) other;
        return Objects.equals(id, that.id);
    }
}
