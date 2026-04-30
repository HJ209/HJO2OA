package com.hjo2oa.shared.tenant;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

public final class TenantContextHolder {

    private static final ThreadLocal<TenantRequestContext> CONTEXT = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void set(TenantRequestContext context) {
        if (context == null) {
            clear();
            return;
        }
        CONTEXT.set(context);
    }

    public static Optional<TenantRequestContext> current() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static TenantRequestContext requireContext() {
        TenantRequestContext context = CONTEXT.get();
        if (context == null) {
            throw new IllegalStateException("Tenant context is not bound to current thread");
        }
        return context;
    }

    public static Optional<UUID> currentTenantId() {
        return current().map(TenantRequestContext::tenantId);
    }

    public static UUID requireTenantId() {
        return requireContext().requireTenant().tenantId();
    }

    public static TenantRequestContext capture() {
        return CONTEXT.get();
    }

    public static Scope bind(TenantRequestContext context) {
        TenantRequestContext previous = CONTEXT.get();
        set(context);
        return new Scope(previous);
    }

    public static Runnable wrap(Runnable delegate) {
        TenantRequestContext captured = capture();
        return () -> {
            try (Scope ignored = bind(captured)) {
                delegate.run();
            }
        };
    }

    public static <T> Callable<T> wrap(Callable<T> delegate) {
        TenantRequestContext captured = capture();
        return () -> {
            try (Scope ignored = bind(captured)) {
                return delegate.call();
            }
        };
    }

    public static <T> Supplier<T> wrap(Supplier<T> delegate) {
        TenantRequestContext captured = capture();
        return () -> {
            try (Scope ignored = bind(captured)) {
                return delegate.get();
            }
        };
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static final class Scope implements AutoCloseable {

        private final TenantRequestContext previous;
        private boolean closed;

        private Scope(TenantRequestContext previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            if (previous == null) {
                clear();
            } else {
                set(previous);
            }
            closed = true;
        }
    }
}
