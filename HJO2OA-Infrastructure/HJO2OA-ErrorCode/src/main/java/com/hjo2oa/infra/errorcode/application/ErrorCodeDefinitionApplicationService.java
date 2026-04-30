package com.hjo2oa.infra.errorcode.application;

import com.hjo2oa.infra.errorcode.domain.ErrorCodeDefinition;
import com.hjo2oa.infra.errorcode.domain.ErrorCodeDefinitionRepository;
import com.hjo2oa.infra.errorcode.domain.ErrorCodeDefinitionView;
import com.hjo2oa.infra.errorcode.domain.ErrorCodeUpdatedEvent;
import com.hjo2oa.infra.errorcode.domain.ErrorSeverity;
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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class ErrorCodeDefinitionApplicationService {

    private final ErrorCodeDefinitionRepository repository;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;
    private final List<ErrorCodeCacheInvalidator> cacheInvalidators;

    @Autowired
    public ErrorCodeDefinitionApplicationService(
            ErrorCodeDefinitionRepository repository,
            DomainEventPublisher domainEventPublisher,
            ObjectProvider<ErrorCodeCacheInvalidator> cacheInvalidators
    ) {
        this(repository, domainEventPublisher, Clock.systemUTC(), cacheInvalidators.orderedStream().toList());
    }

    public ErrorCodeDefinitionApplicationService(
            ErrorCodeDefinitionRepository repository,
            DomainEventPublisher domainEventPublisher
    ) {
        this(repository, domainEventPublisher, Clock.systemUTC(), List.of());
    }

    public ErrorCodeDefinitionApplicationService(
            ErrorCodeDefinitionRepository repository,
            DomainEventPublisher domainEventPublisher,
            Clock clock
    ) {
        this(repository, domainEventPublisher, clock, List.of());
    }

    public ErrorCodeDefinitionApplicationService(
            ErrorCodeDefinitionRepository repository,
            DomainEventPublisher domainEventPublisher,
            Clock clock,
            List<ErrorCodeCacheInvalidator> cacheInvalidators
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.domainEventPublisher = Objects.requireNonNull(domainEventPublisher, "domainEventPublisher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.cacheInvalidators = List.copyOf(cacheInvalidators);
    }

    public ErrorCodeDefinitionView defineCode(ErrorCodeDefinitionCommands.DefineCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        repository.findByCode(command.code()).ifPresent(existing -> {
            throw new BizException(SharedErrorDescriptors.CONFLICT, "错误码编码已存在");
        });
        Instant now = now();
        ErrorCodeDefinition definition = ErrorCodeDefinition.create(
                command.code(),
                command.moduleCode(),
                command.category(),
                command.severity(),
                command.httpStatus(),
                command.messageKey(),
                command.retryable(),
                now
        );
        ErrorCodeDefinition saved = repository.save(definition);
        domainEventPublisher.publish(ErrorCodeUpdatedEvent.from(saved, "CREATED", now));
        invalidateCaches();
        return saved.toView();
    }

    public ErrorCodeDefinitionView defineCode(
            String code,
            String moduleCode,
            ErrorSeverity severity,
            int httpStatus,
            String messageKey,
            String category,
            boolean retryable
    ) {
        return defineCode(new ErrorCodeDefinitionCommands.DefineCommand(
                code,
                moduleCode,
                severity,
                httpStatus,
                messageKey,
                category,
                retryable
        ));
    }

    public ErrorCodeDefinitionView deprecateCode(UUID codeId) {
        return deprecateCode(new ErrorCodeDefinitionCommands.DeprecateCommand(codeId));
    }

    public ErrorCodeDefinitionView deprecateCode(ErrorCodeDefinitionCommands.DeprecateCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        ErrorCodeDefinition definition = loadRequired(command.codeId());
        Instant now = now();
        ErrorCodeDefinition updated = definition.deprecate(now);
        if (updated == definition) {
            return definition.toView();
        }
        ErrorCodeDefinition saved = repository.save(updated);
        domainEventPublisher.publish(ErrorCodeUpdatedEvent.from(saved, "DEPRECATED", now));
        invalidateCaches();
        return saved.toView();
    }

    public ErrorCodeDefinitionView updateSeverity(UUID codeId, ErrorSeverity severity) {
        return updateSeverity(new ErrorCodeDefinitionCommands.UpdateSeverityCommand(codeId, severity));
    }

    public ErrorCodeDefinitionView updateSeverity(ErrorCodeDefinitionCommands.UpdateSeverityCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        ErrorCodeDefinition definition = loadRequired(command.codeId());
        ensureMutable(definition);
        Instant now = now();
        ErrorCodeDefinition updated = definition.updateSeverity(command.severity(), now);
        if (updated == definition) {
            return definition.toView();
        }
        ErrorCodeDefinition saved = repository.save(updated);
        domainEventPublisher.publish(ErrorCodeUpdatedEvent.from(saved, "SEVERITY_UPDATED", now));
        invalidateCaches();
        return saved.toView();
    }

    public ErrorCodeDefinitionView updateHttpStatus(UUID codeId, int httpStatus) {
        return updateHttpStatus(new ErrorCodeDefinitionCommands.UpdateHttpStatusCommand(codeId, httpStatus));
    }

    public ErrorCodeDefinitionView updateHttpStatus(ErrorCodeDefinitionCommands.UpdateHttpStatusCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        ErrorCodeDefinition definition = loadRequired(command.codeId());
        ensureMutable(definition);
        Instant now = now();
        ErrorCodeDefinition updated = definition.updateHttpStatus(command.httpStatus(), now);
        if (updated == definition) {
            return definition.toView();
        }
        ErrorCodeDefinition saved = repository.save(updated);
        domainEventPublisher.publish(ErrorCodeUpdatedEvent.from(saved, "HTTP_STATUS_UPDATED", now));
        invalidateCaches();
        return saved.toView();
    }

    public ErrorCodeDefinitionView updateDefinition(ErrorCodeDefinitionCommands.UpdateDefinitionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        ErrorCodeDefinition definition = loadRequired(command.codeId());
        ensureMutable(definition);
        Instant now = now();
        ErrorCodeDefinition updated = definition.updateMetadata(
                command.category(),
                command.severity(),
                command.httpStatus(),
                command.messageKey(),
                command.retryable(),
                now
        );
        ErrorCodeDefinition saved = repository.save(updated);
        domainEventPublisher.publish(ErrorCodeUpdatedEvent.from(saved, "UPDATED", now));
        invalidateCaches();
        return saved.toView();
    }

    public List<ErrorCodeDefinitionView> queryByModule(String moduleCode) {
        return repository.findByModule(moduleCode).stream()
                .sorted(Comparator.comparing(ErrorCodeDefinition::code))
                .map(ErrorCodeDefinition::toView)
                .toList();
    }

    public Optional<ErrorCodeDefinitionView> queryByCode(String code) {
        return repository.findByCode(code).map(ErrorCodeDefinition::toView);
    }

    public List<ErrorCodeDefinitionView> listAll() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(ErrorCodeDefinition::moduleCode).thenComparing(ErrorCodeDefinition::code))
                .map(ErrorCodeDefinition::toView)
                .toList();
    }

    private ErrorCodeDefinition loadRequired(UUID codeId) {
        return repository.findAll().stream()
                .filter(definition -> definition.id().equals(codeId))
                .findFirst()
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "错误码不存在"));
    }

    private void ensureMutable(ErrorCodeDefinition definition) {
        if (definition.deprecated()) {
            throw new BizException(SharedErrorDescriptors.CONFLICT, "已废弃错误码不允许继续修改");
        }
    }

    private Instant now() {
        return clock.instant();
    }

    private void invalidateCaches() {
        cacheInvalidators.forEach(ErrorCodeCacheInvalidator::invalidateErrorCodeCaches);
    }
}
