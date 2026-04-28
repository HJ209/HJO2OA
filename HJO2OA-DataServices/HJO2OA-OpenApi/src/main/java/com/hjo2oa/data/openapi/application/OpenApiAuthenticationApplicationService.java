package com.hjo2oa.data.openapi.application;

import com.hjo2oa.data.openapi.domain.ApiCredentialGrant;
import com.hjo2oa.data.openapi.domain.AuthenticatedOpenApiInvocationContext;
import com.hjo2oa.data.openapi.domain.OpenApiAuthType;
import com.hjo2oa.data.openapi.domain.OpenApiEndpoint;
import com.hjo2oa.data.openapi.domain.OpenApiEndpointRepository;
import com.hjo2oa.data.openapi.domain.OpenApiErrorDescriptors;
import com.hjo2oa.data.openapi.domain.OpenApiHttpMethod;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OpenApiAuthenticationApplicationService {

    public static final String TENANT_HEADER = "X-Tenant-Id";
    public static final String CLIENT_CODE_HEADER = "X-Client-Code";
    public static final String CLIENT_SECRET_HEADER = "X-Client-Secret";
    public static final String API_VERSION_HEADER = "X-Api-Version";
    public static final String SIGNATURE_HEADER = "X-Signature";
    public static final String TIMESTAMP_HEADER = "X-Timestamp";
    public static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final OpenApiEndpointRepository endpointRepository;
    private final Clock clock;
    @Autowired
    public OpenApiAuthenticationApplicationService(OpenApiEndpointRepository endpointRepository) {
        this(endpointRepository, Clock.systemUTC());
    }

    public OpenApiAuthenticationApplicationService(OpenApiEndpointRepository endpointRepository, Clock clock) {
        this.endpointRepository = Objects.requireNonNull(endpointRepository, "endpointRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public AuthenticatedOpenApiInvocationContext authenticate(HttpServletRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        Instant now = clock.instant();
        String requestId = resolveRequestId(request);
        String tenantId = requiredHeader(request, TENANT_HEADER, "tenant header is required");
        String version = resolveVersion(request);
        OpenApiHttpMethod httpMethod = OpenApiHttpMethod.valueOf(request.getMethod().toUpperCase());
        String path = request.getRequestURI();

        OpenApiEndpoint endpoint = endpointRepository.findByPathMethodAndVersion(tenantId, path, httpMethod, version)
                .filter(candidate -> candidate.isCallableAt(now))
                .orElseThrow(() -> new BizException(OpenApiErrorDescriptors.OPEN_API_NOT_CALLABLE, "API version is not callable"));

        String clientCode = requiredHeader(request, CLIENT_CODE_HEADER, "client code header is required");
        ApiCredentialGrant credentialGrant = endpoint.credentialFor(clientCode)
                .orElseThrow(() -> new BizException(OpenApiErrorDescriptors.CREDENTIAL_NOT_FOUND, "Credential grant not found"));
        switch (credentialGrant.effectiveStatus(now)) {
            case ACTIVE -> {
            }
            case EXPIRED -> throw new BizException(OpenApiErrorDescriptors.CREDENTIAL_EXPIRED, "Credential has expired");
            case REVOKED -> throw new BizException(OpenApiErrorDescriptors.CREDENTIAL_NOT_FOUND, "Credential has been revoked");
        }

        authenticateByType(request, endpoint, credentialGrant, version);
        return new AuthenticatedOpenApiInvocationContext(requestId, tenantId, clientCode, endpoint, credentialGrant, now);
    }

    public String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(ResponseMetaFactory.REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        return requestId;
    }

    public String resolveVersion(HttpServletRequest request) {
        String version = request.getHeader(API_VERSION_HEADER);
        if (version == null || version.isBlank()) {
            version = request.getParameter("version");
        }
        return version == null || version.isBlank() ? "v1" : version.trim();
    }

    private void authenticateByType(
            HttpServletRequest request,
            OpenApiEndpoint endpoint,
            ApiCredentialGrant credentialGrant,
            String version
    ) {
        switch (endpoint.authType()) {
            case APP_KEY -> {
                String providedSecret = requiredHeader(request, CLIENT_SECRET_HEADER, "client secret header is required");
                if (!credentialGrant.secretRef().equals(providedSecret)) {
                    throw new BizException(OpenApiErrorDescriptors.CREDENTIAL_NOT_FOUND, "Client secret is invalid");
                }
            }
            case INTERNAL -> {
                String token = requiredHeader(request, INTERNAL_TOKEN_HEADER, "internal token header is required");
                if (!credentialGrant.secretRef().equals(token)) {
                    throw new BizException(OpenApiErrorDescriptors.CREDENTIAL_NOT_FOUND, "Internal token is invalid");
                }
            }
            case SIGNATURE -> validateSignature(request, endpoint, credentialGrant, version);
            case OAUTH2 -> throw new BizException(OpenApiErrorDescriptors.AUTH_TYPE_UNSUPPORTED, "OAuth2 is not implemented in current phase");
        }
    }

    private void validateSignature(
            HttpServletRequest request,
            OpenApiEndpoint endpoint,
            ApiCredentialGrant credentialGrant,
            String version
    ) {
        requiredHeader(request, CLIENT_SECRET_HEADER, "client secret header is required");
        String timestamp = requiredHeader(request, TIMESTAMP_HEADER, "timestamp header is required");
        String signature = requiredHeader(request, SIGNATURE_HEADER, "signature header is required");
        String canonical = String.join(
                "\n",
                Optional.ofNullable(request.getHeader(CLIENT_CODE_HEADER)).orElse(""),
                request.getMethod().toUpperCase(),
                request.getRequestURI(),
                version,
                timestamp,
                credentialGrant.secretRef()
        );
        String expectedSignature = sha256(canonical);
        if (!expectedSignature.equalsIgnoreCase(signature)) {
            throw new BizException(OpenApiErrorDescriptors.SIGNATURE_INVALID, "Signature validation failed");
        }
        String providedSecret = request.getHeader(CLIENT_SECRET_HEADER);
        if (!credentialGrant.secretRef().equals(providedSecret)) {
            throw new BizException(OpenApiErrorDescriptors.CREDENTIAL_NOT_FOUND, "Client secret is invalid");
        }
        if (endpoint.authType() != OpenApiAuthType.SIGNATURE) {
            throw new BizException(OpenApiErrorDescriptors.AUTH_TYPE_UNSUPPORTED, "Endpoint does not support signature mode");
        }
    }

    private String requiredHeader(HttpServletRequest request, String headerName, String message) {
        String value = request.getHeader(headerName);
        if (value == null || value.isBlank()) {
            throw new BizException(OpenApiErrorDescriptors.CREDENTIAL_NOT_FOUND, message);
        }
        return value.trim();
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is required", ex);
        }
    }
}
