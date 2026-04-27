package com.hjo2oa.msg.ecosystem.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hjo2oa.msg.ecosystem.domain.AuthMode;
import com.hjo2oa.msg.ecosystem.domain.CallbackAuditRecord;
import com.hjo2oa.msg.ecosystem.domain.CallbackAuditRecordView;
import com.hjo2oa.msg.ecosystem.domain.EcosystemIntegration;
import com.hjo2oa.msg.ecosystem.domain.EcosystemIntegrationRepository;
import com.hjo2oa.msg.ecosystem.domain.HealthStatus;
import com.hjo2oa.msg.ecosystem.domain.IntegrationStatus;
import com.hjo2oa.msg.ecosystem.domain.IntegrationType;
import com.hjo2oa.msg.ecosystem.domain.VerifyResult;
import com.hjo2oa.shared.kernel.BizException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class EcosystemIntegrationApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-04-27T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void verifiesSignedEmailCallbackAndKeepsDuplicateIdempotent() {
        InMemoryRepository repository = new InMemoryRepository();
        UUID tenantId = UUID.randomUUID();
        EcosystemIntegration integration = enabledEmailIntegration(tenantId);
        repository.saveIntegration(integration);
        EcosystemIntegrationApplicationService service = service(repository, "email-secret");
        String payload = "{\"messageId\":\"m-1\",\"status\":\"delivered\"}";
        String idempotencyKey = "callback-1";
        String signature = signature("email-secret", tenantId, integration.id(), "delivery", idempotencyKey, payload);

        CallbackAuditRecordView first = service.verifyCallback(new EcosystemIntegrationCommands.VerifyCallbackCommand(
                tenantId,
                integration.id(),
                "delivery",
                idempotencyKey,
                signature,
                payload
        ));
        CallbackAuditRecordView duplicate =
                service.verifyCallback(new EcosystemIntegrationCommands.VerifyCallbackCommand(
                        tenantId,
                        integration.id(),
                        "delivery",
                        idempotencyKey,
                        signature,
                        payload
                ));

        assertThat(first.verifyResult()).isEqualTo(VerifyResult.PASSED);
        assertThat(duplicate.id()).isEqualTo(first.id());
        assertThat(repository.findCallbackAudits(integration.id())).hasSize(1);
    }

    @Test
    void recordsFailedAuditWhenSignatureIsInvalid() {
        InMemoryRepository repository = new InMemoryRepository();
        UUID tenantId = UUID.randomUUID();
        EcosystemIntegration integration = enabledEmailIntegration(tenantId);
        repository.saveIntegration(integration);
        EcosystemIntegrationApplicationService service = service(repository, "email-secret");

        assertThatThrownBy(() -> service.verifyCallback(new EcosystemIntegrationCommands.VerifyCallbackCommand(
                tenantId,
                integration.id(),
                "delivery",
                "callback-2",
                "sha256=bad",
                "{}"
        ))).isInstanceOf(BizException.class);

        List<CallbackAuditRecord> audits = repository.findCallbackAudits(integration.id());
        assertThat(audits).hasSize(1);
        assertThat(audits.get(0).verifyResult()).isEqualTo(VerifyResult.FAILED);
    }

    @Test
    void rejectsCrossTenantCallbackBeforeAudit() {
        InMemoryRepository repository = new InMemoryRepository();
        EcosystemIntegration integration = enabledEmailIntegration(UUID.randomUUID());
        repository.saveIntegration(integration);
        EcosystemIntegrationApplicationService service = service(repository, "email-secret");

        assertThatThrownBy(() -> service.verifyCallback(new EcosystemIntegrationCommands.VerifyCallbackCommand(
                UUID.randomUUID(),
                integration.id(),
                "delivery",
                "callback-3",
                "sha256=bad",
                "{}"
        ))).isInstanceOf(BizException.class);
        assertThat(repository.findCallbackAudits(integration.id())).isEmpty();
    }

    private EcosystemIntegrationApplicationService service(InMemoryRepository repository, String secret) {
        return new EcosystemIntegrationApplicationService(repository, configRef -> Optional.of(secret), CLOCK);
    }

    private EcosystemIntegration enabledEmailIntegration(UUID tenantId) {
        return EcosystemIntegration.create(
                        UUID.randomUUID(),
                        IntegrationType.EMAIL,
                        "Email callback",
                        AuthMode.SIGNATURE,
                        "/callbacks/email",
                        "HMAC_SHA256",
                        "config:email",
                        tenantId,
                        NOW
                )
                .changeStatus(IntegrationStatus.ENABLED, NOW)
                .updateHealth(HealthStatus.HEALTHY, null, NOW);
    }

    private String signature(
            String secret,
            UUID tenantId,
            UUID integrationId,
            String callbackType,
            String idempotencyKey,
            String payload
    ) {
        try {
            String signingText = tenantId + "\n" + integrationId + "\n"
                    + callbackType + "\n" + idempotencyKey + "\n" + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return "sha256=" + HexFormat.of().formatHex(mac.doFinal(signingText.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static final class InMemoryRepository implements EcosystemIntegrationRepository {

        private final Map<UUID, EcosystemIntegration> integrations = new HashMap<>();
        private final Map<UUID, CallbackAuditRecord> audits = new HashMap<>();

        @Override
        public EcosystemIntegration saveIntegration(EcosystemIntegration integration) {
            integrations.put(integration.id(), integration);
            return integration;
        }

        @Override
        public Optional<EcosystemIntegration> findIntegrationById(UUID integrationId) {
            return Optional.ofNullable(integrations.get(integrationId));
        }

        @Override
        public List<EcosystemIntegration> findIntegrations(UUID tenantId, IntegrationType integrationType) {
            return integrations.values().stream()
                    .filter(integration -> integration.tenantId().equals(tenantId))
                    .filter(integration -> integrationType == null || integration.integrationType() == integrationType)
                    .toList();
        }

        @Override
        public CallbackAuditRecord saveCallbackAudit(CallbackAuditRecord record) {
            audits.put(record.id(), record);
            return record;
        }

        @Override
        public Optional<CallbackAuditRecord> findCallbackAudit(UUID integrationId, String idempotencyKey) {
            return audits.values().stream()
                    .filter(audit -> audit.integrationId().equals(integrationId))
                    .filter(audit -> audit.idempotencyKey().equals(idempotencyKey))
                    .findFirst();
        }

        @Override
        public List<CallbackAuditRecord> findCallbackAudits(UUID integrationId) {
            return audits.values().stream()
                    .filter(audit -> audit.integrationId().equals(integrationId))
                    .sorted(Comparator.comparing(CallbackAuditRecord::occurredAt).reversed())
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
    }
}
