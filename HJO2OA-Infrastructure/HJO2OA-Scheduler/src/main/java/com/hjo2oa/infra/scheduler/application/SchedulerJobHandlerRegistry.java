package com.hjo2oa.infra.scheduler.application;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class SchedulerJobHandlerRegistry {

    private final Map<String, SchedulerJobHandler> handlersByName;

    public SchedulerJobHandlerRegistry(Collection<SchedulerJobHandler> handlers) {
        this.handlersByName = handlers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        handler -> normalize(handler.handlerName()),
                        Function.identity(),
                        (left, right) -> {
                            throw new IllegalStateException("Duplicate scheduler handler: " + left.handlerName());
                        }
                ));
    }

    public Optional<SchedulerJobHandler> find(String handlerName) {
        String normalized = normalize(handlerName);
        return Optional.ofNullable(handlersByName.get(normalized));
    }

    private static String normalize(String value) {
        Objects.requireNonNull(value, "handlerName must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("handlerName must not be blank");
        }
        return normalized;
    }
}
