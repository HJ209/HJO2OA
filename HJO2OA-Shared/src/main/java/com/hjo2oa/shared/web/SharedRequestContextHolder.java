package com.hjo2oa.shared.web;

import java.util.Optional;

public final class SharedRequestContextHolder {

    private static final ThreadLocal<SharedRequestContext> CONTEXT = new ThreadLocal<>();

    private SharedRequestContextHolder() {
    }

    public static void set(SharedRequestContext context) {
        CONTEXT.set(context);
    }

    public static Optional<SharedRequestContext> current() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
