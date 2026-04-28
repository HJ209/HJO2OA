package com.hjo2oa.data.governance.application;

import com.hjo2oa.data.governance.application.GovernanceCommands.GovernancePagedResult;
import com.hjo2oa.data.governance.application.GovernanceCommands.PublishServiceVersionCommand;
import com.hjo2oa.data.governance.application.GovernanceCommands.RegisterServiceVersionCommand;
import com.hjo2oa.data.governance.application.GovernanceCommands.UpsertAlertRuleCommand;
import com.hjo2oa.data.governance.application.GovernanceCommands.UpsertGovernanceProfileCommand;
import com.hjo2oa.data.governance.application.GovernanceCommands.UpsertHealthCheckRuleCommand;
import com.hjo2oa.data.governance.application.GovernanceCommands.VersionListQuery;
import com.hjo2oa.data.governance.application.GovernanceCommands.DeprecateServiceVersionCommand;
import com.hjo2oa.data.governance.domain.AlertRule;
import com.hjo2oa.data.governance.domain.GovernanceActionAuditRecord;
import com.hjo2oa.data.governance.domain.GovernanceProfile;
import com.hjo2oa.data.governance.domain.GovernanceProfileRepository;
import com.hjo2oa.data.governance.domain.GovernanceRuntimeRepository;
import com.hjo2oa.data.governance.domain.HealthCheckRule;
import com.hjo2oa.data.governance.domain.ServiceVersionRecord;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceActionResult;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceActionType;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceProfileStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceScopeType;
import com.hjo2oa.data.governance.domain.GovernanceTypes.ServiceVersionStatus;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GovernanceProfileApplicationService {

    private final GovernanceProfileRepository profileRepository;
    private final GovernanceRuntimeRepository runtimeRepository;
    private final Clock clock;
    @Autowired
    public GovernanceProfileApplicationService(
            GovernanceProfileRepository profileRepository,
            GovernanceRuntimeRepository runtimeRepository
    ) {
        this(profileRepository, runtimeRepository, Clock.systemUTC());
    }
    public GovernanceProfileApplicationService(
            GovernanceProfileRepository profileRepository,
            GovernanceRuntimeRepository runtimeRepository,
            Clock clock
    ) {
        this.profileRepository = Objects.requireNonNull(profileRepository, "profileRepository must not be null");
        this.runtimeRepository = Objects.requireNonNull(runtimeRepository, "runtimeRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public GovernanceProfile upsertProfile(UpsertGovernanceProfileCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Instant now = now();
        GovernanceProfile profile = profileRepository.findByCode(command.tenantId(), command.code())
                .map(existing -> existing.updatePolicies(
                        command.slaPolicyJson(),
                        command.alertPolicyJson(),
                        command.status() == null ? existing.status() : command.status(),
                        now
                ))
                .orElseGet(() -> GovernanceProfile.create(
                        UUID.randomUUID().toString(),
                        command.code(),
                        Objects.requireNonNull(command.scopeType(), "scopeType must not be null"),
                        command.targetCode(),
                        command.slaPolicyJson(),
                        command.alertPolicyJson(),
                        command.tenantId(),
                        now
                ).updatePolicies(
                        command.slaPolicyJson(),
                        command.alertPolicyJson(),
                        command.status() == null ? GovernanceProfileStatus.ACTIVE : command.status(),
                        now
                ));
        GovernanceProfile saved = profileRepository.save(profile);
        saveAudit(saved, command.operatorId(), command.operatorName(), command.requestId(), GovernanceActionType.UPSERT_PROFILE, null);
        return saved;
    }

    public List<GovernanceProfile> listProfiles(
            String tenantId,
            GovernanceScopeType scopeType,
            GovernanceProfileStatus status
    ) {
        return profileRepository.findByTenant(tenantId).stream()
                .filter(profile -> scopeType == null || profile.scopeType() == scopeType)
                .filter(profile -> status == null || profile.status() == status)
                .sorted(Comparator.comparing(GovernanceProfile::code).thenComparing(GovernanceProfile::targetCode))
                .toList();
    }

    public HealthCheckRule upsertHealthCheckRule(UpsertHealthCheckRuleCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        GovernanceProfile profile = requireProfile(command.tenantId(), command.profileCode());
        Instant now = now();
        HealthCheckRule savedRule = profile.healthCheckRules().stream()
                .filter(existingRule -> existingRule.ruleCode().equals(command.ruleCode()))
                .findFirst()
                .map(existingRule -> existingRule.update(
                        command.ruleName(),
                        command.checkType(),
                        command.severity(),
                        command.status(),
                        command.metricName(),
                        command.comparisonOperator(),
                        command.thresholdValue(),
                        command.windowMinutes(),
                        command.dedupMinutes(),
                        command.scheduleExpression(),
                        command.strategyJson(),
                        now
                ))
                .orElseGet(() -> new HealthCheckRule(
                        UUID.randomUUID().toString(),
                        profile.governanceId(),
                        command.ruleCode(),
                        command.ruleName(),
                        command.checkType(),
                        command.severity(),
                        command.status(),
                        command.metricName(),
                        command.comparisonOperator(),
                        command.thresholdValue(),
                        command.windowMinutes(),
                        command.dedupMinutes(),
                        command.scheduleExpression(),
                        command.strategyJson(),
                        now,
                        now
                ));
        List<HealthCheckRule> nextRules = replaceRule(profile.healthCheckRules(), savedRule, HealthCheckRule::ruleCode);
        profileRepository.save(profile.replaceHealthCheckRules(nextRules, now));
        saveAudit(profile, command.operatorId(), command.operatorName(), command.requestId(), GovernanceActionType.UPSERT_HEALTH_RULE, savedRule.ruleCode());
        return savedRule;
    }

    public List<HealthCheckRule> listHealthCheckRules(
            String tenantId,
            String profileCode,
            com.hjo2oa.data.governance.domain.GovernanceTypes.HealthCheckRuleStatus status
    ) {
        GovernanceProfile profile = requireProfile(tenantId, profileCode);
        return profile.healthCheckRules().stream()
                .filter(rule -> status == null || rule.status() == status)
                .sorted(Comparator.comparing(HealthCheckRule::ruleCode))
                .toList();
    }

    public AlertRule upsertAlertRule(UpsertAlertRuleCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        GovernanceProfile profile = requireProfile(command.tenantId(), command.profileCode());
        Instant now = now();
        if (command.sourceRuleCode() != null) {
            boolean sourceRuleExists = profile.healthCheckRules().stream()
                    .anyMatch(rule -> rule.ruleCode().equals(command.sourceRuleCode()));
            if (!sourceRuleExists) {
                throw new BizException(
                        GovernanceErrorDescriptors.DEPENDENCY_NOT_READY,
                        "Health check rule not found for alert rule: " + command.sourceRuleCode()
                );
            }
        }
        AlertRule savedRule = profile.alertRules().stream()
                .filter(existingRule -> existingRule.ruleCode().equals(command.ruleCode()))
                .findFirst()
                .map(existingRule -> existingRule.update(
                        command.ruleName(),
                        command.sourceRuleCode(),
                        command.metricName(),
                        command.alertType(),
                        command.alertLevel(),
                        command.status(),
                        command.comparisonOperator(),
                        command.thresholdValue(),
                        command.dedupMinutes(),
                        command.escalationMinutes(),
                        command.notificationPolicyJson(),
                        command.strategyJson(),
                        now
                ))
                .orElseGet(() -> new AlertRule(
                        UUID.randomUUID().toString(),
                        profile.governanceId(),
                        command.ruleCode(),
                        command.ruleName(),
                        command.sourceRuleCode(),
                        command.metricName(),
                        command.alertType(),
                        command.alertLevel(),
                        command.status(),
                        command.comparisonOperator(),
                        command.thresholdValue(),
                        command.dedupMinutes(),
                        command.escalationMinutes(),
                        command.notificationPolicyJson(),
                        command.strategyJson(),
                        now,
                        now
                ));
        List<AlertRule> nextRules = replaceRule(profile.alertRules(), savedRule, AlertRule::ruleCode);
        profileRepository.save(profile.replaceAlertRules(nextRules, now));
        saveAudit(profile, command.operatorId(), command.operatorName(), command.requestId(), GovernanceActionType.UPSERT_ALERT_RULE, savedRule.ruleCode());
        return savedRule;
    }

    public List<AlertRule> listAlertRules(
            String tenantId,
            String profileCode,
            com.hjo2oa.data.governance.domain.GovernanceTypes.AlertRuleStatus status
    ) {
        GovernanceProfile profile = requireProfile(tenantId, profileCode);
        return profile.alertRules().stream()
                .filter(rule -> status == null || rule.status() == status)
                .sorted(Comparator.comparing(AlertRule::ruleCode))
                .toList();
    }

    public ServiceVersionRecord registerVersion(RegisterServiceVersionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        GovernanceProfile profile = requireProfile(command.tenantId(), command.profileCode());
        Optional<ServiceVersionRecord> existing = findVersionRecord(profile, command.version());
        if (existing.isPresent()) {
            return existing.orElseThrow();
        }
        Instant now = now();
        ServiceVersionRecord record = ServiceVersionRecord.register(
                UUID.randomUUID().toString(),
                profile.governanceId(),
                command.targetType() == null ? profile.scopeType() : command.targetType(),
                command.targetCode() == null ? profile.targetCode() : command.targetCode(),
                command.version(),
                command.compatibilityNote(),
                command.changeSummary(),
                command.operatorId(),
                command.approvalNote(),
                UUID.randomUUID().toString(),
                now
        );
        List<ServiceVersionRecord> nextRecords = new ArrayList<>(profile.serviceVersionRecords());
        nextRecords.add(record);
        profileRepository.save(profile.replaceServiceVersionRecords(nextRecords, now));
        saveAudit(profile, command.operatorId(), command.operatorName(), command.requestId(), GovernanceActionType.REGISTER_VERSION, record.version());
        return record;
    }

    public ServiceVersionRecord publishVersion(PublishServiceVersionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        GovernanceProfile profile = requireProfile(command.tenantId(), command.profileCode());
        ServiceVersionRecord existing = findVersionRecord(profile, command.version())
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Version record not found"));
        if (existing.status() == ServiceVersionStatus.PUBLISHED) {
            return existing;
        }
        if (existing.status() != ServiceVersionStatus.REGISTERED) {
            throw new BizException(
                    GovernanceErrorDescriptors.INVALID_STATUS_TRANSITION,
                    "Only registered version can be published"
            );
        }
        Instant now = now();
        ServiceVersionRecord published = existing.publish(command.operatorId(), command.approvalNote(), UUID.randomUUID().toString(), now);
        List<ServiceVersionRecord> nextRecords = replaceRule(profile.serviceVersionRecords(), published, ServiceVersionRecord::version);
        profileRepository.save(profile.replaceServiceVersionRecords(nextRecords, now));
        saveAudit(profile, command.operatorId(), command.operatorName(), command.requestId(), GovernanceActionType.PUBLISH_VERSION, published.version());
        return published;
    }

    public ServiceVersionRecord deprecateVersion(DeprecateServiceVersionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        GovernanceProfile profile = requireProfile(command.tenantId(), command.profileCode());
        ServiceVersionRecord existing = findVersionRecord(profile, command.version())
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Version record not found"));
        if (existing.status() == ServiceVersionStatus.DEPRECATED) {
            return existing;
        }
        if (existing.status() != ServiceVersionStatus.PUBLISHED) {
            throw new BizException(
                    GovernanceErrorDescriptors.INVALID_STATUS_TRANSITION,
                    "Only published version can be deprecated"
            );
        }
        Instant now = now();
        ServiceVersionRecord deprecated = existing.deprecate(
                command.operatorId(),
                command.approvalNote(),
                UUID.randomUUID().toString(),
                now
        );
        List<ServiceVersionRecord> nextRecords = replaceRule(profile.serviceVersionRecords(), deprecated, ServiceVersionRecord::version);
        profileRepository.save(profile.replaceServiceVersionRecords(nextRecords, now));
        saveAudit(profile, command.operatorId(), command.operatorName(), command.requestId(), GovernanceActionType.DEPRECATE_VERSION, deprecated.version());
        return deprecated;
    }

    public GovernancePagedResult<ServiceVersionRecord> listVersionRecords(VersionListQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        List<ServiceVersionRecord> items = profileRepository.findByTenant(query.tenantId()).stream()
                .flatMap(profile -> profile.serviceVersionRecords().stream())
                .filter(record -> query.targetType() == null || record.targetType() == query.targetType())
                .filter(record -> query.targetCode() == null || record.targetCode().equals(query.targetCode()))
                .filter(record -> query.status() == null || record.status() == query.status())
                .sorted(Comparator.comparing(ServiceVersionRecord::registeredAt).reversed()
                        .thenComparing(ServiceVersionRecord::version).reversed())
                .toList();
        return new GovernancePagedResult<>(items, items.size());
    }

    private GovernanceProfile requireProfile(String tenantId, String profileCode) {
        return profileRepository.findByCode(tenantId, profileCode)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Governance profile not found: " + profileCode
                ));
    }

    private Optional<ServiceVersionRecord> findVersionRecord(GovernanceProfile profile, String version) {
        return profile.serviceVersionRecords().stream()
                .filter(record -> record.version().equals(version))
                .findFirst();
    }

    private <T> List<T> replaceRule(List<T> items, T replacement, java.util.function.Function<T, String> keyExtractor) {
        List<T> nextItems = new ArrayList<>();
        boolean replaced = false;
        String replacementKey = keyExtractor.apply(replacement);
        for (T item : items) {
            if (keyExtractor.apply(item).equals(replacementKey)) {
                nextItems.add(replacement);
                replaced = true;
            } else {
                nextItems.add(item);
            }
        }
        if (!replaced) {
            nextItems.add(replacement);
        }
        return List.copyOf(nextItems);
    }

    private void saveAudit(
            GovernanceProfile profile,
            String operatorId,
            String operatorName,
            String requestId,
            GovernanceActionType actionType,
            String payload
    ) {
        runtimeRepository.saveAudit(new GovernanceActionAuditRecord(
                UUID.randomUUID().toString(),
                profile.governanceId(),
                profile.scopeType(),
                profile.targetCode(),
                actionType,
                GovernanceActionResult.COMPLETED,
                normalizeOperator(operatorId),
                operatorName,
                null,
                resolveRequestId(requestId),
                payload,
                "OK",
                null,
                now(),
                now()
        ));
    }

    private String resolveRequestId(String requestId) {
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId.trim();
    }

    private String normalizeOperator(String operatorId) {
        return operatorId == null || operatorId.isBlank() ? "system" : operatorId.trim();
    }

    private Instant now() {
        return clock.instant();
    }
}
