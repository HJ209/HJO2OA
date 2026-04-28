package com.hjo2oa.msg.ecosystem.application;

import com.hjo2oa.msg.ecosystem.domain.CallbackAuditRecord;
import com.hjo2oa.msg.ecosystem.domain.CallbackAuditRecordView;
import com.hjo2oa.msg.ecosystem.domain.EcosystemIntegration;
import com.hjo2oa.msg.ecosystem.domain.EcosystemIntegrationRepository;
import com.hjo2oa.msg.ecosystem.domain.EcosystemIntegrationView;
import com.hjo2oa.msg.ecosystem.domain.HealthStatus;
import com.hjo2oa.msg.ecosystem.domain.IntegrationAvailabilityView;
import com.hjo2oa.msg.ecosystem.domain.IntegrationStatus;
import com.hjo2oa.msg.ecosystem.domain.IntegrationType;
import com.hjo2oa.msg.ecosystem.domain.VerifyResult;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class EcosystemIntegrationApplicationService {

    private static final int PAYLOAD_SUMMARY_LIMIT = 1000;

    private final EcosystemIntegrationRepository repository;
    private final IntegrationSecretResolver secretResolver;
    private final Clock clock;
    @Autowired
    public EcosystemIntegrationApplicationService(
            EcosystemIntegrationRepository repository,
            IntegrationSecretResolver secretResolver
    ) {
        this(repository, secretResolver, Clock.systemUTC());
    }
    public EcosystemIntegrationApplicationService(
            EcosystemIntegrationRepository repository,
            IntegrationSecretResolver secretResolver,
            Clock clock
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.secretResolver = Objects.requireNonNull(secretResolver, "secretResolver must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public EcosystemIntegrationView createIntegration(
            EcosystemIntegrationCommands.CreateIntegrationCommand command
    ) {
        Objects.requireNonNull(command, "command must not be null");
        EcosystemIntegration integration = EcosystemIntegration.create(
                UUID.randomUUID(),
                command.integrationType(),
                command.displayName(),
                command.authMode(),
                command.callbackUrl(),
                command.signAlgorithm(),
                command.configRef(),
                command.tenantId(),
                now()
        );
        return repository.saveIntegration(integration).toView();
    }

    public EcosystemIntegrationView updateIntegration(
            EcosystemIntegrationCommands.UpdateIntegrationCommand command
    ) {
        Objects.requireNonNull(command, "command must not be null");
        EcosystemIntegration integration = loadIntegration(command.integrationId());
        return repository.saveIntegration(integration.updateConfiguration(
                command.displayName(),
                command.authMode(),
                command.callbackUrl(),
                command.signAlgorithm(),
                command.configRef(),
                now()
        )).toView();
    }

    public EcosystemIntegrationView changeStatus(
            EcosystemIntegrationCommands.ChangeIntegrationStatusCommand command
    ) {
        Objects.requireNonNull(command, "command must not be null");
        return repository.saveIntegration(loadIntegration(command.integrationId())
                .changeStatus(command.status(), now()))
                .toView();
    }

    public EcosystemIntegrationView testConnection(UUID integrationId) {
        EcosystemIntegration integration = loadIntegration(integrationId);
        HealthStatus healthStatus = secretResolver.resolveSecret(integration.configRef()).isPresent()
                ? HealthStatus.HEALTHY
                : HealthStatus.DEGRADED;
        String errorSummary = healthStatus == HealthStatus.HEALTHY ? null : "Config reference is not resolvable";
        return repository.saveIntegration(integration.updateHealth(healthStatus, errorSummary, now())).toView();
    }

    public EcosystemIntegrationView updateHealth(EcosystemIntegrationCommands.UpdateHealthCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        return repository.saveIntegration(loadIntegration(command.integrationId())
                .updateHealth(command.healthStatus(), command.errorSummary(), now()))
                .toView();
    }

    public List<EcosystemIntegrationView> listIntegrations(UUID tenantId, IntegrationType integrationType) {
        return repository.findIntegrations(tenantId, integrationType).stream()
                .map(EcosystemIntegration::toView)
                .toList();
    }

    public IntegrationAvailabilityView availability(UUID integrationId) {
        return loadIntegration(integrationId).toAvailabilityView();
    }

    public CallbackAuditRecordView verifyCallback(EcosystemIntegrationCommands.VerifyCallbackCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        EcosystemIntegration integration = loadIntegration(command.integrationId());
        requireTenant(command.tenantId(), integration);
        String idempotencyKey = requireText(command.idempotencyKey(), "idempotencyKey");
        return repository.findCallbackAudit(integration.id(), idempotencyKey)
                .map(this::existingCallbackAudit)
                .orElseGet(() -> verifyAndAudit(command, integration, idempotencyKey).toView());
    }

    public List<CallbackAuditRecordView> callbackAudits(UUID integrationId) {
        return repository.findCallbackAudits(integrationId).stream()
                .map(CallbackAuditRecord::toView)
                .toList();
    }

    private CallbackAuditRecordView existingCallbackAudit(CallbackAuditRecord record) {
        if (record.verifyResult() == VerifyResult.FAILED) {
            throw new BizException(SharedErrorDescriptors.UNAUTHORIZED, record.errorMessage());
        }
        return record.toView();
    }

    private CallbackAuditRecord verifyAndAudit(
            EcosystemIntegrationCommands.VerifyCallbackCommand command,
            EcosystemIntegration integration,
            String idempotencyKey
    ) {
        String payload = command.payload() == null ? "" : command.payload();
        String payloadDigest = sha256(payload);
        String errorMessage = verifyFailure(command, integration, idempotencyKey, payload);
        VerifyResult result = errorMessage == null ? VerifyResult.PASSED : VerifyResult.FAILED;
        CallbackAuditRecord record = CallbackAuditRecord.create(
                UUID.randomUUID(),
                integration.id(),
                command.callbackType(),
                result,
                payloadSummary(payload, payloadDigest),
                errorMessage,
                idempotencyKey,
                payloadDigest,
                now()
        );
        CallbackAuditRecord saved = repository.saveCallbackAudit(record);
        if (result == VerifyResult.FAILED) {
            throw new BizException(SharedErrorDescriptors.UNAUTHORIZED, errorMessage);
        }
        return saved;
    }

    private String verifyFailure(
            EcosystemIntegrationCommands.VerifyCallbackCommand command,
            EcosystemIntegration integration,
            String idempotencyKey,
            String payload
    ) {
        if (integration.status() != IntegrationStatus.ENABLED) {
            return "Integration is not enabled";
        }
        if (!"HMAC_SHA256".equalsIgnoreCase(integration.signAlgorithm())) {
            return "Unsupported signature algorithm";
        }
        String signature = command.signature();
        if (signature == null || signature.isBlank()) {
            return "signature must not be blank";
        }
        String secret = secretResolver.resolveSecret(integration.configRef()).orElse(null);
        if (secret == null || secret.isBlank()) {
            return "Config reference is not resolvable";
        }
        String signingText = command.tenantId() + "\n" + integration.id() + "\n"
                + command.callbackType() + "\n" + idempotencyKey + "\n" + payload;
        String expected = hmacSha256Hex(secret, signingText);
        String normalized = signature.startsWith("sha256=") ? signature.substring("sha256=".length()) : signature;
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), normalized.getBytes(StandardCharsets.UTF_8))
                ? null
                : "Signature verification failed";
    }

    private void requireTenant(UUID tenantId, EcosystemIntegration integration) {
        if (!integration.tenantId().equals(tenantId)) {
            throw new BizException(SharedErrorDescriptors.FORBIDDEN, "Tenant is not allowed for integration");
        }
    }

    private EcosystemIntegration loadIntegration(UUID integrationId) {
        return repository.findIntegrationById(integrationId)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Ecosystem integration not found"
                ));
    }

    private String payloadSummary(String payload, String digest) {
        String normalized = payload.length() <= PAYLOAD_SUMMARY_LIMIT
                ? payload
                : payload.substring(0, PAYLOAD_SUMMARY_LIMIT);
        return "{\"sha256\":\"" + digest + "\",\"sample\":\"" + escapeJson(normalized) + "\"}";
    }

    private String hmacSha256Hex(String secret, String signingText) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(signingText.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to verify callback signature", ex);
        }
    }

    private String sha256(String payload) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to digest callback payload", ex);
        }
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BizException(SharedErrorDescriptors.BAD_REQUEST, fieldName + " must not be blank");
        }
        return value.trim();
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private Instant now() {
        return clock.instant();
    }
}
