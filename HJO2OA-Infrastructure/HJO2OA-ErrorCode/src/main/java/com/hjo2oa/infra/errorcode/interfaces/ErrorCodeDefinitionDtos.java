package com.hjo2oa.infra.errorcode.interfaces;

import com.hjo2oa.infra.errorcode.domain.ErrorSeverity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class ErrorCodeDefinitionDtos {

    private ErrorCodeDefinitionDtos() {
    }

    public record DefineRequest(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 32) String moduleCode,
            @Size(max = 64) String category,
            @NotNull ErrorSeverity severity,
            @Min(100) @Max(599) int httpStatus,
            @NotBlank @Size(max = 256) String messageKey,
            Boolean retryable
    ) {
    }

    public record UpdateSeverityRequest(@NotNull ErrorSeverity severity) {
    }

    public record UpdateHttpStatusRequest(@Min(100) @Max(599) int httpStatus) {
    }

    public record UpdateDefinitionRequest(
            @Size(max = 64) String category,
            @NotNull ErrorSeverity severity,
            @Min(100) @Max(599) int httpStatus,
            @NotBlank @Size(max = 256) String messageKey,
            Boolean retryable
    ) {
    }

    public record DetailResponse(
            UUID id,
            String code,
            String moduleCode,
            String category,
            ErrorSeverity severity,
            int httpStatus,
            String messageKey,
            String message,
            boolean retryable,
            boolean deprecated,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
