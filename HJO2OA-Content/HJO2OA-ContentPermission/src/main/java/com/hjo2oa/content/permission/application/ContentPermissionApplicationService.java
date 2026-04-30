package com.hjo2oa.content.permission.application;

import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.messaging.DomainEvent;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentPermissionApplicationService {

    private final PublicationScopeRepository repository;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    @Autowired
    public ContentPermissionApplicationService(PublicationScopeRepository repository) {
        this(repository, event -> {
        }, Clock.systemUTC());
    }

    public ContentPermissionApplicationService(
            PublicationScopeRepository repository,
            DomainEventPublisher eventPublisher,
            Clock clock
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Transactional
    public List<PublicationScopeRuleView> replaceScopes(ReplacePublicationScopeCommand command) {
        validate(command.tenantId(), command.publicationId(), command.articleId());
        int nextVersion = repository.latestScopeVersion(command.tenantId(), command.publicationId()).orElse(0) + 1;
        List<PublicationScopeRuleRecord> rules = toRecords(command, nextVersion);
        List<PublicationScopeRuleRecord> current = repository.findByPublication(command.tenantId(), command.publicationId());
        if (sameRules(current, rules)) {
            return current.stream().map(ContentPermissionApplicationService::toView).toList();
        }
        repository.replaceScopes(command.tenantId(), command.publicationId(), command.articleId(), rules);
        eventPublisher.publish(new ContentVisibilityChangedEvent(
                UUID.randomUUID(),
                "content.visibility.changed",
                clock.instant(),
                command.tenantId().toString(),
                command.articleId(),
                command.publicationId(),
                command.operatorId()
        ));
        return rules.stream().map(ContentPermissionApplicationService::toView).toList();
    }

    @Transactional(readOnly = true)
    public boolean canRead(UUID tenantId, UUID articleId, ContentSubjectContext subject) {
        if (tenantId == null || articleId == null) {
            return false;
        }
        List<PublicationScopeRuleRecord> rules = repository.findByArticle(tenantId, articleId);
        return evaluate(rules, subject).allowed();
    }

    @Transactional(readOnly = true)
    public PermissionDecision evaluateArticle(UUID tenantId, UUID articleId, ContentSubjectContext subject) {
        if (tenantId == null || articleId == null) {
            throw new BizException(SharedErrorDescriptors.TENANT_REQUIRED, "Tenant and article id are required");
        }
        return evaluate(repository.findByArticle(tenantId, articleId), subject);
    }

    @Transactional(readOnly = true)
    public List<PublicationScopeRuleView> scopes(UUID tenantId, UUID publicationId) {
        if (tenantId == null) {
            throw new BizException(SharedErrorDescriptors.TENANT_REQUIRED, "Tenant id is required");
        }
        return repository.findByPublication(tenantId, publicationId).stream()
                .map(ContentPermissionApplicationService::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PublicationScopeRuleRecord> articleScopeRecords(UUID tenantId, UUID articleId) {
        if (tenantId == null || articleId == null) {
            return List.of();
        }
        return repository.findByArticle(tenantId, articleId);
    }

    private PermissionDecision evaluate(List<PublicationScopeRuleRecord> rules, ContentSubjectContext subject) {
        if (rules == null || rules.isEmpty()) {
            return new PermissionDecision(true, "PUBLIC", List.of());
        }
        List<PublicationScopeRuleRecord> matched = rules.stream()
                .filter(rule -> matches(rule, subject))
                .sorted(Comparator.comparing(PublicationScopeRuleRecord::sortOrder))
                .toList();
        boolean denied = matched.stream().anyMatch(rule -> rule.effect() == ScopeEffect.DENY);
        if (denied) {
            return new PermissionDecision(false, "DENY_RULE_MATCHED", matched.stream().map(ContentPermissionApplicationService::toView).toList());
        }
        boolean allowed = matched.stream().anyMatch(rule -> rule.effect() == ScopeEffect.ALLOW);
        return new PermissionDecision(
                allowed,
                allowed ? "ALLOW_RULE_MATCHED" : "NO_RULE_MATCHED",
                matched.stream().map(ContentPermissionApplicationService::toView).toList()
        );
    }

    private static boolean matches(PublicationScopeRuleRecord rule, ContentSubjectContext subject) {
        if (rule.subjectType() == ScopeSubjectType.ALL) {
            return true;
        }
        if (subject == null || rule.subjectId() == null) {
            return false;
        }
        return switch (rule.subjectType()) {
            case PERSON -> rule.subjectId().equals(subject.personId());
            case ASSIGNMENT -> rule.subjectId().equals(subject.assignmentId());
            case POSITION -> rule.subjectId().equals(subject.positionId());
            case DEPARTMENT -> rule.subjectId().equals(subject.departmentId());
            case ROLE -> subject.roleIds().contains(rule.subjectId());
            case ALL -> true;
        };
    }

    private static List<PublicationScopeRuleRecord> toRecords(ReplacePublicationScopeCommand command, int scopeVersion) {
        if (command.rules() == null || command.rules().isEmpty()) {
            return List.of(new PublicationScopeRuleRecord(
                    UUID.randomUUID(),
                    command.publicationId(),
                    command.articleId(),
                    command.tenantId(),
                    ScopeSubjectType.ALL,
                    null,
                    ScopeEffect.ALLOW,
                    0,
                    scopeVersion,
                    Instant.now()
            ));
        }
        int[] index = {0};
        return command.rules().stream()
                .map(input -> new PublicationScopeRuleRecord(
                        UUID.randomUUID(),
                        command.publicationId(),
                        command.articleId(),
                        command.tenantId(),
                        input.subjectType() == null ? ScopeSubjectType.ALL : input.subjectType(),
                        input.subjectType() == ScopeSubjectType.ALL ? null : input.subjectId(),
                        input.effect() == null ? ScopeEffect.ALLOW : input.effect(),
                        input.sortOrder() == null ? index[0]++ : input.sortOrder(),
                        scopeVersion,
                        Instant.now()
                ))
                .toList();
    }

    private static PublicationScopeRuleView toView(PublicationScopeRuleRecord rule) {
        return new PublicationScopeRuleView(
                rule.id(),
                rule.publicationId(),
                rule.articleId(),
                rule.subjectType(),
                rule.subjectId(),
                rule.effect(),
                rule.sortOrder(),
                rule.scopeVersion(),
                rule.createdAt()
        );
    }

    private static void validate(UUID tenantId, UUID publicationId, UUID articleId) {
        if (tenantId == null) {
            throw new BizException(SharedErrorDescriptors.TENANT_REQUIRED, "Tenant id is required");
        }
        if (publicationId == null || articleId == null) {
            throw new BizException(SharedErrorDescriptors.VALIDATION_ERROR, "Publication and article id are required");
        }
    }

    private static boolean sameRules(List<PublicationScopeRuleRecord> current, List<PublicationScopeRuleRecord> requested) {
        List<PublicationScopeRuleRecord> left = current == null ? List.of() : current;
        List<PublicationScopeRuleRecord> right = requested == null ? List.of() : requested;
        if (left.size() != right.size()) {
            return false;
        }
        for (int index = 0; index < left.size(); index++) {
            PublicationScopeRuleRecord currentRule = left.get(index);
            PublicationScopeRuleRecord requestedRule = right.get(index);
            if (currentRule.subjectType() != requestedRule.subjectType()
                    || !Objects.equals(currentRule.subjectId(), requestedRule.subjectId())
                    || currentRule.effect() != requestedRule.effect()
                    || currentRule.sortOrder() != requestedRule.sortOrder()) {
                return false;
            }
        }
        return true;
    }

    public enum ScopeSubjectType {
        ALL,
        PERSON,
        ASSIGNMENT,
        POSITION,
        DEPARTMENT,
        ROLE
    }

    public enum ScopeEffect {
        ALLOW,
        DENY
    }

    public record ContentSubjectContext(
            UUID personId,
            UUID assignmentId,
            UUID positionId,
            UUID departmentId,
            Set<UUID> roleIds
    ) {

        public ContentSubjectContext {
            roleIds = roleIds == null ? Set.of() : Set.copyOf(roleIds);
        }
    }

    public record PublicationScopeRuleInput(
            ScopeSubjectType subjectType,
            UUID subjectId,
            ScopeEffect effect,
            Integer sortOrder
    ) {
    }

    public record PublicationScopeRuleRecord(
            UUID id,
            UUID publicationId,
            UUID articleId,
            UUID tenantId,
            ScopeSubjectType subjectType,
            UUID subjectId,
            ScopeEffect effect,
            int sortOrder,
            int scopeVersion,
            Instant createdAt
    ) {
    }

    public record ReplacePublicationScopeCommand(
            UUID tenantId,
            UUID publicationId,
            UUID articleId,
            UUID operatorId,
            List<PublicationScopeRuleInput> rules,
            String idempotencyKey
    ) {
    }

    public record PublicationScopeRuleView(
            UUID id,
            UUID publicationId,
            UUID articleId,
            ScopeSubjectType subjectType,
            UUID subjectId,
            ScopeEffect effect,
            int sortOrder,
            int scopeVersion,
            Instant createdAt
    ) {
    }

    public record PermissionDecision(
            boolean allowed,
            String decision,
            List<PublicationScopeRuleView> matchedRules
    ) {

        public PermissionDecision {
            matchedRules = matchedRules == null ? List.of() : List.copyOf(matchedRules);
        }
    }

    public record ContentVisibilityChangedEvent(
            UUID eventId,
            String eventType,
            Instant occurredAt,
            String tenantId,
            UUID articleId,
            UUID publicationId,
            UUID operatorId
    ) implements DomainEvent {
    }
}
