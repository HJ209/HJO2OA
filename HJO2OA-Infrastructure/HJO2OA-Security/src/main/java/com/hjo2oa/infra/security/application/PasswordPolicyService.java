package com.hjo2oa.infra.security.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.infra.security.domain.SecurityPolicy;
import com.hjo2oa.infra.security.domain.SecurityPolicyRepository;
import com.hjo2oa.infra.security.domain.SecurityPolicyStatus;
import com.hjo2oa.infra.security.domain.SecurityPolicyType;
import com.hjo2oa.shared.kernel.BizException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordPolicyService {

    private static final PasswordPolicy DEFAULT_POLICY = new PasswordPolicy(
            8,
            true,
            true,
            true,
            true,
            true,
            5
    );

    private final SecurityPolicyRepository securityPolicyRepository;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public PasswordPolicyService(
            SecurityPolicyRepository securityPolicyRepository,
            ObjectMapper objectMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.securityPolicyRepository = Objects.requireNonNull(
                securityPolicyRepository,
                "securityPolicyRepository must not be null"
        );
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder must not be null");
    }

    public PasswordValidationResult validate(
            String policyCode,
            String password,
            String username,
            List<String> passwordHistory
    ) {
        String normalizedPassword = requireText(password, "password");
        PasswordPolicy policy = resolvePolicy(policyCode);
        List<String> violations = new ArrayList<>();
        if (normalizedPassword.length() < policy.minLength()) {
            violations.add("PASSWORD_TOO_SHORT");
        }
        if (policy.requireUppercase() && normalizedPassword.chars().noneMatch(Character::isUpperCase)) {
            violations.add("PASSWORD_REQUIRES_UPPERCASE");
        }
        if (policy.requireLowercase() && normalizedPassword.chars().noneMatch(Character::isLowerCase)) {
            violations.add("PASSWORD_REQUIRES_LOWERCASE");
        }
        if (policy.requireDigit() && normalizedPassword.chars().noneMatch(Character::isDigit)) {
            violations.add("PASSWORD_REQUIRES_DIGIT");
        }
        if (policy.requireSpecial() && normalizedPassword.chars().noneMatch(this::isSpecial)) {
            violations.add("PASSWORD_REQUIRES_SPECIAL");
        }
        if (policy.disallowUsername()
                && username != null
                && !username.isBlank()
                && normalizedPassword.toLowerCase(Locale.ROOT).contains(username.toLowerCase(Locale.ROOT))) {
            violations.add("PASSWORD_CONTAINS_USERNAME");
        }
        if (matchesHistory(normalizedPassword, passwordHistory, policy.historyCount())) {
            violations.add("PASSWORD_REUSED");
        }
        return new PasswordValidationResult(violations.isEmpty(), violations);
    }

    public void assertAccepted(String policyCode, String password, String username, List<String> passwordHistory) {
        PasswordValidationResult result = validate(policyCode, password, username, passwordHistory);
        if (!result.accepted()) {
            throw new BizException(
                    SecurityErrorDescriptors.SECURITY_PASSWORD_POLICY_REJECTED,
                    "Password rejected by policy: " + String.join(",", result.violations())
            );
        }
    }

    private PasswordPolicy resolvePolicy(String policyCode) {
        return securityPolicyRepository.findAll().stream()
                .filter(policy -> policy.policyType() == SecurityPolicyType.PASSWORD)
                .filter(policy -> policy.status() == SecurityPolicyStatus.ACTIVE)
                .filter(policy -> policyCode == null || policy.policyCode().equalsIgnoreCase(policyCode))
                .findFirst()
                .map(this::parsePolicy)
                .orElse(DEFAULT_POLICY);
    }

    private PasswordPolicy parsePolicy(SecurityPolicy policy) {
        try {
            JsonNode root = objectMapper.readTree(policy.configSnapshot());
            return new PasswordPolicy(
                    root.path("minLength").asInt(DEFAULT_POLICY.minLength()),
                    root.path("requireUppercase").asBoolean(DEFAULT_POLICY.requireUppercase()),
                    root.path("requireLowercase").asBoolean(DEFAULT_POLICY.requireLowercase()),
                    root.path("requireDigit").asBoolean(DEFAULT_POLICY.requireDigit()),
                    root.path("requireSpecial").asBoolean(DEFAULT_POLICY.requireSpecial()),
                    root.path("disallowUsername").asBoolean(DEFAULT_POLICY.disallowUsername()),
                    root.path("historyCount").asInt(DEFAULT_POLICY.historyCount())
            );
        } catch (Exception ex) {
            throw new BizException(
                    SecurityErrorDescriptors.SECURITY_POLICY_RULE_VIOLATION,
                    "Password policy configSnapshot is invalid",
                    ex
            );
        }
    }

    private boolean matchesHistory(String password, List<String> passwordHistory, int historyCount) {
        if (passwordHistory == null || passwordHistory.isEmpty() || historyCount <= 0) {
            return false;
        }
        return passwordHistory.stream()
                .limit(historyCount)
                .anyMatch(previous -> matchesPassword(password, previous));
    }

    private boolean matchesPassword(String password, String previous) {
        if (previous == null || previous.isBlank()) {
            return false;
        }
        if (previous.startsWith("{") || previous.startsWith("$2a$")
                || previous.startsWith("$2b$") || previous.startsWith("$2y$")) {
            return passwordEncoder.matches(password, previous);
        }
        return password.equals(previous);
    }

    private boolean isSpecial(int codePoint) {
        return !Character.isLetterOrDigit(codePoint);
    }

    private String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private record PasswordPolicy(
            int minLength,
            boolean requireUppercase,
            boolean requireLowercase,
            boolean requireDigit,
            boolean requireSpecial,
            boolean disallowUsername,
            int historyCount
    ) {
    }
}
