package com.hjo2oa.data.connector.infrastructure;

import com.hjo2oa.shared.web.ApiResponse;
import java.util.function.Supplier;
import org.springframework.http.ResponseEntity;

public interface ApiIdempotencyStore {

    <T> ResponseEntity<ApiResponse<T>> execute(
            String scope,
            String idempotencyKey,
            Supplier<ApiResponse<T>> action
    );
}
