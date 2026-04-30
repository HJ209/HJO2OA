package com.hjo2oa.infra.errorcode.application;

import com.hjo2oa.infra.errorcode.domain.ErrorSeverity;
import java.util.Objects;
import java.util.UUID;

public final class ErrorCodeDefinitionCommands {

    private ErrorCodeDefinitionCommands() {
    }

    public record DefineCommand(
            String code,
            String moduleCode,
            ErrorSeverity severity,
            int httpStatus,
            String messageKey,
            String category,
            boolean retryable
    ) {

        public DefineCommand {
            Objects.requireNonNull(severity, "severity must not be null");
        }
    }

    public record DeprecateCommand(UUID codeId) {

        public DeprecateCommand {
            Objects.requireNonNull(codeId, "codeId must not be null");
        }
    }

    public record UpdateSeverityCommand(UUID codeId, ErrorSeverity severity) {

        public UpdateSeverityCommand {
            Objects.requireNonNull(codeId, "codeId must not be null");
            Objects.requireNonNull(severity, "severity must not be null");
        }
    }

    public record UpdateHttpStatusCommand(UUID codeId, int httpStatus) {

        public UpdateHttpStatusCommand {
            Objects.requireNonNull(codeId, "codeId must not be null");
        }
    }

    public record UpdateDefinitionCommand(
            UUID codeId,
            ErrorSeverity severity,
            int httpStatus,
            String messageKey,
            String category,
            boolean retryable
    ) {

        public UpdateDefinitionCommand {
            Objects.requireNonNull(codeId, "codeId must not be null");
            Objects.requireNonNull(severity, "severity must not be null");
        }
    }
}
