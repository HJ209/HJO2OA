package com.hjo2oa.msg.ecosystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hjo2oa.msg.ecosystem.application.EcosystemIntegrationApplicationService;
import com.hjo2oa.msg.ecosystem.application.EcosystemIntegrationCommands;
import com.hjo2oa.msg.ecosystem.domain.AuthMode;
import com.hjo2oa.msg.ecosystem.domain.CallbackAuditRecord;
import com.hjo2oa.msg.ecosystem.domain.CallbackAuditRecordView;
import com.hjo2oa.msg.ecosystem.domain.EcosystemIntegration;
import com.hjo2oa.msg.ecosystem.domain.EcosystemIntegrationRepository;
import com.hjo2oa.msg.ecosystem.domain.EcosystemIntegrationView;
import com.hjo2oa.msg.ecosystem.domain.HealthStatus;
import com.hjo2oa.msg.ecosystem.domain.IntegrationStatus;
import com.hjo2oa.msg.ecosystem.domain.IntegrationType;
import com.hjo2oa.msg.ecosystem.domain.VerifyResult;
import com.hjo2oa.shared.kernel.BizException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class EcosystemIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-04-27T00:00:00Z");
    private static final String SECRET = "email-secret";

    private final InMemoryRepository repository = new InMemoryRepository();
    private final EcosystemIntegrationApplicationService service = new EcosystemIntegrationApplicationService(
            repository,
            configRef -> "config/ecosystem/email".equals(configRef) ? Optional.of(SECRET) : Optional.empty(),
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void shouldCoverConfigurationHealthCallbackIdempotencyAndSignatureFailure() {
        UUID tenantId = UUID.randomUUID();
        EcosystemIntegrationView created = service.createIntegration(
                new EcosystemIntegrationCommands.CreateIntegrationCommand(
                        IntegrationType.EMAIL,
                        "Email integration",
                        AuthMode.SIGNATURE,
                        "https://example.test/callback",
                        "HMAC_SHA256",
                        "config/ecosystem/email",
                        tenantId
                )
        );
        assertThat(created.status()).isEqualTo(IntegrationStatus.DRAFT);
        assertThat(created.configRef()).isEqualTo("config/ecosystem/email");

        EcosystemIntegrationView healthy = service.testConnection(created.id());
        assertThat(healthy.healthStatus()).isEqualTo(HealthStatus.HEALTHY);
        assertThat(healthy.lastCheckAt()).isEqualTo(NOW);
        assertThat(healthy.lastErrorSummary()).isNull();

        service.changeStatus(new EcosystemIntegrationCommands.ChangeIntegrationStatusCommand(
                created.id(),
                IntegrationStatus.ENABLED
        ));
        String payload = "{\"messageId\":\"m-1\",\"status\":\"delivered\"}";
        String idempotencyKey = "mail-callback-1";
        String signature = signature(tenantId, created.id(), "delivery", idempotencyKey, payload);

        CallbackAuditRecordView passed = service.verifyCallback(new EcosystemIntegrationCommands.VerifyCallbackCommand(
                tenantId,
                created.id(),
                "delivery",
                idempotencyKey,
                signature,
                payload
        ));
        CallbackAuditRecordView duplicate = service.verifyCallback(new EcosystemIntegrationCommands.VerifyCallbackCommand(
                tenantId,
                created.id(),
                "delivery",
                idempotencyKey,
                signature,
                payload
        ));
        assertThat(passed.verifyResult()).isEqualTo(VerifyResult.PASSED);
        assertThat(duplicate.id()).isEqualTo(passed.id());
        assertThat(repository.findCallbackAudits(created.id())).hasSize(1);

        assertThatThrownBy(() -> service.verifyCallback(new EcosystemIntegrationCommands.VerifyCallbackCommand(
                tenantId,
                created.id(),
                "delivery",
                "mail-callback-2",
                "sha256=bad",
                payload
        ))).isInstanceOf(BizException.class);
        assertThat(repository.findCallbackAudits(created.id()))
                .extracting(CallbackAuditRecord::verifyResult)
                .containsExactlyInAnyOrder(VerifyResult.FAILED, VerifyResult.PASSED);
    }

    private String signature(
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
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return "sha256=" + java.util.HexFormat.of()
                    .formatHex(mac.doFinal(signingText.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static final class InMemoryRepository implements EcosystemIntegrationRepository {
        private final Map<UUID, EcosystemIntegration> integrations = new LinkedHashMap<>();
        private final Map<UUID, CallbackAuditRecord> audits = new LinkedHashMap<>();

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
                    .toList();
        }
    }
}
