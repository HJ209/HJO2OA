package com.hjo2oa.infra.security.application;

import java.util.List;

public record PasswordValidationResult(
        boolean accepted,
        List<String> violations
) {

    public PasswordValidationResult {
        violations = violations == null ? List.of() : List.copyOf(violations);
    }
}
