package com.hjo2oa.data.common.application.assembler;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public interface BaseAssembler<S, T> {

    T toTarget(S source);

    default List<T> toTargets(Collection<S> sources) {
        if (sources == null || sources.isEmpty()) {
            return List.of();
        }
        return sources.stream()
                .filter(Objects::nonNull)
                .map(this::toTarget)
                .toList();
    }
}
