package com.hjo2oa.infra.security.infrastructure.jwt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class JwtTokenProvider {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String JWT_ALGORITHM = "HS256";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final byte[] secret;
    private final long expirationMillis;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public JwtTokenProvider(String secret, long expirationMillis, ObjectMapper objectMapper) {
        this(secret, expirationMillis, objectMapper, Clock.systemUTC());
    }

    JwtTokenProvider(String secret, long expirationMillis, ObjectMapper objectMapper, Clock clock) {
        String normalizedSecret = requireText(secret, "secret");
        if (expirationMillis <= 0) {
            throw new IllegalArgumentException("expirationMillis must be positive");
        }
        this.secret = normalizedSecret.getBytes(StandardCharsets.UTF_8);
        this.expirationMillis = expirationMillis;
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public String generateToken(String personId, String username, List<String> roles, String tenantId) {
        return generateToken(
                personId,
                username,
                roles,
                tenantId,
                null,
                null,
                null,
                null,
                null,
                0L
        );
    }

    public String generateToken(
            String personId,
            String username,
            List<String> roles,
            String tenantId,
            String accountId,
            String currentAssignmentId,
            String currentPositionId,
            String currentOrganizationId,
            String currentDepartmentId,
            long permissionSnapshotVersion
    ) {
        Instant now = clock.instant();
        Instant expiresAt = now.plusMillis(expirationMillis);
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", JWT_ALGORITHM);
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", requireText(personId, "personId"));
        payload.put("username", requireText(username, "username"));
        payload.put("roles", List.copyOf(roles == null ? List.of() : roles));
        payload.put("tenantId", requireText(tenantId, "tenantId"));
        putIfPresent(payload, "accountId", accountId);
        putIfPresent(payload, "asgId", currentAssignmentId);
        putIfPresent(payload, "posId", currentPositionId);
        putIfPresent(payload, "orgId", currentOrganizationId);
        putIfPresent(payload, "deptId", currentDepartmentId);
        payload.put("pVer", Math.max(permissionSnapshotVersion, 0L));
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());

        String unsignedToken = base64UrlJson(header) + "." + base64UrlJson(payload);
        return unsignedToken + "." + sign(unsignedToken);
    }

    public boolean validateToken(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public String refreshToken(String token) {
        JwtClaims claims = extractClaims(token);
        return generateToken(
                claims.personId(),
                claims.username(),
                claims.roles(),
                claims.tenantId(),
                claims.accountId(),
                claims.currentAssignmentId(),
                claims.currentPositionId(),
                claims.currentOrganizationId(),
                claims.currentDepartmentId(),
                claims.permissionSnapshotVersion()
        );
    }

    public JwtClaims extractClaims(String token) {
        String[] parts = splitToken(token);
        verifySignature(parts);
        Map<String, Object> payload = parseJson(parts[1]);
        Instant expiresAt = Instant.ofEpochSecond(readLong(payload, "exp"));
        if (!expiresAt.isAfter(clock.instant())) {
            throw new IllegalArgumentException("JWT token has expired");
        }
        return new JwtClaims(
                readString(payload, "sub"),
                readString(payload, "username"),
                readStringList(payload.get("roles")),
                readString(payload, "tenantId"),
                readOptionalString(payload, "accountId"),
                readOptionalString(payload, "asgId"),
                readOptionalString(payload, "posId"),
                readOptionalString(payload, "orgId"),
                readOptionalString(payload, "deptId"),
                readOptionalLong(payload, "pVer"),
                Instant.ofEpochSecond(readLong(payload, "iat")),
                expiresAt
        );
    }

    private void verifySignature(String[] parts) {
        String unsignedToken = parts[0] + "." + parts[1];
        String expectedSignature = sign(unsignedToken);
        if (!constantTimeEquals(expectedSignature, parts[2])) {
            throw new IllegalArgumentException("JWT signature is invalid");
        }
    }

    private String[] splitToken(String token) {
        String normalizedToken = requireText(token, "token");
        String[] parts = normalizedToken.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("JWT token must contain three parts");
        }
        return parts;
    }

    private String base64UrlJson(Map<String, Object> value) {
        try {
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize JWT JSON", ex);
        }
    }

    private Map<String, Object> parseJson(String encodedJson) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(encodedJson);
            return objectMapper.readValue(decoded, MAP_TYPE);
        } catch (Exception ex) {
            throw new IllegalArgumentException("JWT payload is invalid", ex);
        }
    }

    private String sign(String unsignedToken) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret, HMAC_SHA256));
            byte[] signature = mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to sign JWT token", ex);
        }
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
        if (expectedBytes.length != actualBytes.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < expectedBytes.length; i++) {
            result |= expectedBytes[i] ^ actualBytes[i];
        }
        return result == 0;
    }

    private static String readString(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        throw new IllegalArgumentException("JWT claim is missing or invalid: " + key);
    }

    private static String readOptionalString(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value instanceof String text && !text.isBlank() ? text : null;
    }

    private static long readOptionalLong(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private static long readLong(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalArgumentException("JWT claim is missing or invalid: " + key);
    }

    private static List<String> readStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static void putIfPresent(Map<String, Object> payload, String key, String value) {
        if (value != null && !value.isBlank()) {
            payload.put(key, value);
        }
    }
}
