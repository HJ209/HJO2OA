package com.hjo2oa.data.connector.infrastructure;

import com.hjo2oa.shared.web.ApiResponse;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class InMemoryApiIdempotencyStore implements ApiIdempotencyStore {

    private final Map<String, StoredResponse<?>> storedResponses = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T> ResponseEntity<ApiResponse<T>> execute(
            String scope,
            String idempotencyKey,
            Supplier<ApiResponse<T>> action
    ) {
        Objects.requireNonNull(scope, "scope must not be null");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        Objects.requireNonNull(action, "action must not be null");
        String compositeKey = scope + ":" + idempotencyKey;
        StoredResponse<?> existing = storedResponses.get(compositeKey);
        if (existing != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body((ApiResponse<T>) existing.body());
        }
        ApiResponse<T> response = action.get();
        storedResponses.put(compositeKey, new StoredResponse<>(response, Instant.now()));
        return ResponseEntity.ok(response);
    }

    private record StoredResponse<T>(
            ApiResponse<T> body,
            Instant storedAt
    ) {
    }
}
