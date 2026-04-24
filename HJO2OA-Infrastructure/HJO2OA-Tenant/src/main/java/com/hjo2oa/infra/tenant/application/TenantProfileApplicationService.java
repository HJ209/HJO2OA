package com.hjo2oa.infra.tenant.application;

import com.hjo2oa.infra.tenant.domain.IsolationMode;
import com.hjo2oa.infra.tenant.domain.QuotaType;
import com.hjo2oa.infra.tenant.domain.TenantCreatedEvent;
import com.hjo2oa.infra.tenant.domain.TenantInitializedEvent;
import com.hjo2oa.infra.tenant.domain.TenantProfile;
import com.hjo2oa.infra.tenant.domain.TenantProfileRepository;
import com.hjo2oa.infra.tenant.domain.TenantProfileView;
import com.hjo2oa.infra.tenant.domain.TenantQuota;
import com.hjo2oa.infra.tenant.domain.TenantQuotaRepository;
import com.hjo2oa.infra.tenant.domain.TenantQuotaView;
import com.hjo2oa.infra.tenant.domain.TenantQuotaWarningEvent;
import com.hjo2oa.infra.tenant.domain.TenantStatus;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TenantProfileApplicationService {

    private final TenantProfileRepository tenantProfileRepository;
    private final TenantQuotaRepository tenantQuotaRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;

    public TenantProfileApplicationService(
            TenantProfileRepository tenantProfileRepository,
            TenantQuotaRepository tenantQuotaRepository,
            DomainEventPublisher domainEventPublisher
    ) {
        this(tenantProfileRepository, tenantQuotaRepository, domainEventPublisher, Clock.systemUTC());
    }

    public TenantProfileApplicationService(
            TenantProfileRepository tenantProfileRepository,
            TenantQuotaRepository tenantQuotaRepository,
            DomainEventPublisher domainEventPublisher,
            Clock clock
    ) {
        this.tenantProfileRepository = Objects.requireNonNull(
                tenantProfileRepository,
                "tenantProfileRepository must not be null"
        );
        this.tenantQuotaRepository = Objects.requireNonNull(tenantQuotaRepository, "tenantQuotaRepository must not be null");
        this.domainEventPublisher = Objects.requireNonNull(domainEventPublisher, "domainEventPublisher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public TenantProfileView createTenant(
            String code,
            String name,
            IsolationMode isolationMode,
            String packageCode
    ) {
        return createTenant(new TenantProfileCommands.CreateTenantCommand(code, name, isolationMode, packageCode));
    }

    public TenantProfileView createTenant(TenantProfileCommands.CreateTenantCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        tenantProfileRepository.findByCode(null, command.code()).ifPresent(existing -> {
            throw new BizException(SharedErrorDescriptors.CONFLICT, "Tenant code already exists");
        });

        Instant now = now();
        TenantProfile profile = TenantProfile.create(
                UUID.randomUUID(),
                command.code(),
                command.name(),
                command.isolationMode(),
                command.packageCode(),
                null,
                null,
                now
        );
        TenantProfile savedProfile = tenantProfileRepository.save(profile);
        domainEventPublisher.publish(TenantCreatedEvent.from(savedProfile, now));
        return savedProfile.toView();
    }

    public TenantProfileView activateTenant(UUID tenantId) {
        TenantProfile profile = requireTenant(tenantId);
        ensureNotArchived(profile, "Archived tenant cannot be activated");
        TenantProfile updatedProfile = tenantProfileRepository.save(profile.activate(now()));
        return updatedProfile.toView();
    }

    public TenantProfileView initializeTenant(UUID tenantId) {
        TenantProfile profile = requireTenant(tenantId);
        ensureNotArchived(profile, "Archived tenant cannot be initialized");
        TenantProfile initializedProfile = profile.markInitialized(now());
        TenantProfile savedProfile = tenantProfileRepository.save(initializedProfile);
        if (!profile.initialized() && savedProfile.initialized()) {
            domainEventPublisher.publish(TenantInitializedEvent.from(savedProfile, now()));
        }
        return savedProfile.toView();
    }

    public TenantProfileView suspendTenant(UUID tenantId) {
        TenantProfile profile = requireTenant(tenantId);
        ensureNotArchived(profile, "Archived tenant cannot be suspended");
        TenantProfile updatedProfile = tenantProfileRepository.save(profile.suspend(now()));
        return updatedProfile.toView();
    }

    public TenantProfileView archiveTenant(UUID tenantId) {
        TenantProfile profile = requireTenant(tenantId);
        TenantProfile updatedProfile = tenantProfileRepository.save(profile.archive(now()));
        return updatedProfile.toView();
    }

    public TenantQuotaView updateQuota(
            UUID tenantId,
            QuotaType quotaType,
            long limitValue,
            Long warningThreshold
    ) {
        return updateQuota(new TenantProfileCommands.UpdateQuotaCommand(tenantId, quotaType, limitValue, warningThreshold));
    }

    public TenantQuotaView updateQuota(TenantProfileCommands.UpdateQuotaCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        TenantProfile profile = requireTenant(command.tenantId());
        TenantQuota quota = tenantQuotaRepository.findByTenantProfileIdAndQuotaType(profile.id(), command.quotaType())
                .orElseGet(() -> validateQuota(
                        UUID.randomUUID(),
                        profile.id(),
                        command.quotaType(),
                        command.limitValue(),
                        0,
                        command.warningThreshold()
                ));

        TenantQuota updatedQuota = validateQuota(
                quota.id(),
                profile.id(),
                command.quotaType(),
                command.limitValue(),
                quota.usedValue(),
                command.warningThreshold()
        );
        return tenantQuotaRepository.save(updatedQuota).toView();
    }

    public TenantQuotaView checkQuota(UUID tenantId, QuotaType quotaType) {
        return checkQuota(new TenantProfileCommands.CheckQuotaCommand(tenantId, quotaType));
    }

    public TenantQuotaView checkQuota(TenantProfileCommands.CheckQuotaCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        requireTenant(command.tenantId());
        TenantQuota quota = tenantQuotaRepository.findByTenantProfileIdAndQuotaType(command.tenantId(), command.quotaType())
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Tenant quota not found"
                ));
        if (quota.isWarning()) {
            domainEventPublisher.publish(TenantQuotaWarningEvent.from(quota, now()));
        }
        return quota.toView();
    }

    public Optional<TenantProfileView> current(UUID tenantId) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        return tenantProfileRepository.findByTenantId(tenantId).map(TenantProfile::toView);
    }

    public List<TenantProfileView> listActive() {
        return tenantProfileRepository.findAllActive().stream()
                .sorted(Comparator.comparing(TenantProfile::createdAt).thenComparing(TenantProfile::tenantCode))
                .map(TenantProfile::toView)
                .toList();
    }

    public List<TenantQuotaView> listQuotas(UUID tenantId) {
        requireTenant(tenantId);
        return tenantQuotaRepository.findAllByTenantProfileId(tenantId).stream()
                .sorted(Comparator.comparing(TenantQuota::quotaType))
                .map(TenantQuota::toView)
                .toList();
    }

    private TenantProfile requireTenant(UUID tenantId) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        return tenantProfileRepository.findByTenantId(tenantId).orElseThrow(() -> new BizException(
                SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                "Tenant profile not found"
        ));
    }

    private TenantQuota validateQuota(
            UUID quotaId,
            UUID tenantId,
            QuotaType quotaType,
            long limitValue,
            long usedValue,
            Long warningThreshold
    ) {
        try {
            return new TenantQuota(quotaId, tenantId, quotaType, limitValue, usedValue, warningThreshold);
        } catch (IllegalArgumentException ex) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, ex.getMessage(), ex);
        }
    }

    private void ensureNotArchived(TenantProfile profile, String message) {
        if (profile.status() == TenantStatus.ARCHIVED) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, message);
        }
    }

    private Instant now() {
        return clock.instant();
    }
}
