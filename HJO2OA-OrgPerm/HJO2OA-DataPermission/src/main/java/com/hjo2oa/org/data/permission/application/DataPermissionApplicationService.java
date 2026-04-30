package com.hjo2oa.org.data.permission.application;

import com.hjo2oa.org.data.permission.domain.DataPermission;
import com.hjo2oa.org.data.permission.domain.DataPermissionDecisionView;
import com.hjo2oa.org.data.permission.domain.DataPermissionQuery;
import com.hjo2oa.org.data.permission.domain.DataPermissionRepository;
import com.hjo2oa.org.data.permission.domain.DataPermissionView;
import com.hjo2oa.org.data.permission.domain.DataScopeType;
import com.hjo2oa.org.data.permission.domain.FieldPermission;
import com.hjo2oa.org.data.permission.domain.FieldPermissionAction;
import com.hjo2oa.org.data.permission.domain.FieldPermissionDecisionView;
import com.hjo2oa.org.data.permission.domain.FieldPermissionQuery;
import com.hjo2oa.org.data.permission.domain.FieldPermissionView;
import com.hjo2oa.org.data.permission.domain.PermissionEffect;
import com.hjo2oa.org.data.permission.domain.PermissionSubjectType;
import com.hjo2oa.org.data.permission.domain.SubjectReference;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class DataPermissionApplicationService {

    private static final Comparator<DataPermission> ROW_DECISION_ORDER = Comparator
            .comparing((DataPermission policy) -> policy.effect() == PermissionEffect.DENY ? 0 : 1)
            .thenComparing(Comparator.comparingInt(DataPermission::priority).reversed())
            .thenComparing(policy -> subjectSpecificity(policy.subjectType()));

    private static final Comparator<FieldPermission> FIELD_DECISION_ORDER = Comparator
            .comparing((FieldPermission policy) -> policy.effect() == PermissionEffect.DENY ? 0 : 1)
            .thenComparing(policy -> subjectSpecificity(policy.subjectType()));

    private final DataPermissionRepository repository;
    private final Clock clock;
    @Autowired
    public DataPermissionApplicationService(DataPermissionRepository repository) {
        this(repository, Clock.systemUTC());
    }
    public DataPermissionApplicationService(DataPermissionRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public DataPermissionView createRowPolicy(DataPermissionCommands.SaveRowPolicyCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        ensureSupportedRowPolicy(command.scopeType(), command.conditionExpr());
        Instant now = now();
        DataPermission policy = DataPermission.create(
                UUID.randomUUID(),
                command.subjectType(),
                command.subjectId(),
                command.businessObject(),
                command.scopeType(),
                command.conditionExpr(),
                command.effect(),
                command.priority(),
                command.tenantId(),
                now
        );
        return repository.saveRowPolicy(policy).toView();
    }

    public DataPermissionView updateRowPolicy(UUID policyId, DataPermissionCommands.SaveRowPolicyCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        ensureSupportedRowPolicy(command.scopeType(), command.conditionExpr());
        DataPermission current = loadRequiredRowPolicy(policyId);
        ensureSameRowIdentity(current, command);
        DataPermission updated = current.update(
                command.scopeType(),
                command.conditionExpr(),
                command.effect() == null ? PermissionEffect.ALLOW : command.effect(),
                command.priority(),
                now()
        );
        return repository.saveRowPolicy(updated).toView();
    }

    public void deleteRowPolicy(UUID policyId) {
        loadRequiredRowPolicy(policyId);
        repository.deleteRowPolicy(policyId);
    }

    public Optional<DataPermissionView> findRowPolicy(UUID policyId) {
        return repository.findRowPolicyById(policyId).map(DataPermission::toView);
    }

    public List<DataPermissionView> queryRowPolicies(DataPermissionQuery query) {
        return repository.findRowPolicies(query).stream()
                .sorted(Comparator.comparing(DataPermission::businessObject)
                        .thenComparing(DataPermission::subjectType)
                        .thenComparing(DataPermission::subjectId)
                        .thenComparing(Comparator.comparingInt(DataPermission::priority).reversed()))
                .map(DataPermission::toView)
                .toList();
    }

    public FieldPermissionView createFieldPolicy(DataPermissionCommands.SaveFieldPolicyCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        ensureFieldPolicyUnique(null, command);
        FieldPermission policy = FieldPermission.create(
                UUID.randomUUID(),
                command.subjectType(),
                command.subjectId(),
                command.businessObject(),
                command.usageScenario(),
                command.fieldCode(),
                command.action(),
                command.effect(),
                command.tenantId(),
                now()
        );
        return repository.saveFieldPolicy(policy).toView();
    }

    public FieldPermissionView updateFieldPolicy(UUID policyId, DataPermissionCommands.SaveFieldPolicyCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        FieldPermission current = loadRequiredFieldPolicy(policyId);
        ensureSameFieldIdentity(current, command);
        ensureFieldPolicyUnique(policyId, command);
        FieldPermission updated = current.update(
                command.action(),
                command.effect() == null ? PermissionEffect.ALLOW : command.effect(),
                now()
        );
        return repository.saveFieldPolicy(updated).toView();
    }

    public void deleteFieldPolicy(UUID policyId) {
        loadRequiredFieldPolicy(policyId);
        repository.deleteFieldPolicy(policyId);
    }

    public Optional<FieldPermissionView> findFieldPolicy(UUID policyId) {
        return repository.findFieldPolicyById(policyId).map(FieldPermission::toView);
    }

    public List<FieldPermissionView> queryFieldPolicies(FieldPermissionQuery query) {
        return repository.findFieldPolicies(query).stream()
                .sorted(Comparator.comparing(FieldPermission::businessObject)
                        .thenComparing(FieldPermission::usageScenario)
                        .thenComparing(FieldPermission::fieldCode)
                        .thenComparing(FieldPermission::action))
                .map(FieldPermission::toView)
                .toList();
    }

    public DataPermissionDecisionView decideRow(DataPermissionCommands.RowDecisionQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        List<SubjectReference> subjects = requireSubjects(query.subjects());
        List<DataPermission> matched = findMatchedRowPolicies(query.businessObject(), query.tenantId(), subjects);
        Optional<DataPermission> winner = matched.stream().sorted(ROW_DECISION_ORDER).findFirst();
        return winner.map(policy -> new DataPermissionDecisionView(
                        query.businessObject(),
                        policy.effect() == PermissionEffect.ALLOW,
                        policy.scopeType(),
                        policy.conditionExpr(),
                        toSqlCondition(policy, subjects),
                        policy.effect(),
                        matched.stream().map(DataPermission::toView).toList()
                ))
                .orElseGet(() -> new DataPermissionDecisionView(
                        query.businessObject(),
                        false,
                        null,
                        null,
                        "1 = 0",
                        PermissionEffect.DENY,
                        List.of()
                ));
    }

    public FieldPermissionDecisionView decideField(DataPermissionCommands.FieldDecisionQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        List<SubjectReference> subjects = requireSubjects(query.subjects());
        List<FieldPermission> matched = findMatchedFieldPolicies(
                query.businessObject(),
                query.usageScenario(),
                query.tenantId(),
                subjects
        );
        List<String> requestedFields = query.fieldCodes() == null ? List.of() : query.fieldCodes();
        Map<String, Map<FieldPermissionAction, PermissionEffect>> effects = new LinkedHashMap<>();
        Set<String> hiddenFields = new LinkedHashSet<>();
        Set<String> desensitizedFields = new LinkedHashSet<>();
        for (FieldPermission policy : matched.stream().sorted(FIELD_DECISION_ORDER).toList()) {
            if (!requestedFields.isEmpty() && !requestedFields.contains(policy.fieldCode())) {
                continue;
            }
            effects.computeIfAbsent(policy.fieldCode(), ignored -> new EnumMap<>(FieldPermissionAction.class))
                    .putIfAbsent(policy.action(), policy.effect());
            if (policy.action() == FieldPermissionAction.HIDDEN && policy.effect() == PermissionEffect.ALLOW) {
                hiddenFields.add(policy.fieldCode());
            }
            if (policy.action() == FieldPermissionAction.VISIBLE && policy.effect() == PermissionEffect.DENY) {
                hiddenFields.add(policy.fieldCode());
            }
            if (policy.action() == FieldPermissionAction.DESENSITIZED
                    && policy.effect() == PermissionEffect.ALLOW
                    && !hiddenFields.contains(policy.fieldCode())) {
                desensitizedFields.add(policy.fieldCode());
            }
        }
        return new FieldPermissionDecisionView(
                query.businessObject(),
                query.usageScenario(),
                effects,
                hiddenFields,
                desensitizedFields,
                matched.stream().map(FieldPermission::toView).toList()
        );
    }

    private List<DataPermission> findMatchedRowPolicies(
            String businessObject,
            UUID tenantId,
            List<SubjectReference> subjects
    ) {
        List<DataPermission> matched = new ArrayList<>();
        for (SubjectReference subject : subjects) {
            matched.addAll(repository.findRowPolicies(new DataPermissionQuery(
                    subject.subjectType(),
                    subject.subjectId(),
                    businessObject,
                    null,
                    null,
                    tenantId
            )));
        }
        return matched;
    }

    private List<FieldPermission> findMatchedFieldPolicies(
            String businessObject,
            String usageScenario,
            UUID tenantId,
            List<SubjectReference> subjects
    ) {
        List<FieldPermission> matched = new ArrayList<>();
        for (SubjectReference subject : subjects) {
            matched.addAll(repository.findFieldPolicies(new FieldPermissionQuery(
                    subject.subjectType(),
                    subject.subjectId(),
                    businessObject,
                    usageScenario,
                    null,
                    null,
                    null,
                    tenantId
            )));
        }
        return matched;
    }

    private DataPermission loadRequiredRowPolicy(UUID policyId) {
        return repository.findRowPolicyById(policyId)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Row policy not found"));
    }

    private FieldPermission loadRequiredFieldPolicy(UUID policyId) {
        return repository.findFieldPolicyById(policyId)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Field policy not found"));
    }

    private void ensureSupportedRowPolicy(DataScopeType scopeType, String conditionExpr) {
        if ((scopeType == DataScopeType.CONDITION || scopeType == DataScopeType.CUSTOM)
                && DataPermission.normalizeNullable(conditionExpr) == null) {
            throw new BizException(
                    SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                    "conditionExpr is required for CONDITION or CUSTOM scope"
            );
        }
    }

    private void ensureFieldPolicyUnique(UUID currentId, DataPermissionCommands.SaveFieldPolicyCommand command) {
        List<FieldPermission> duplicates = repository.findFieldPolicies(new FieldPermissionQuery(
                command.subjectType(),
                command.subjectId(),
                command.businessObject(),
                command.usageScenario(),
                command.fieldCode(),
                command.action(),
                null,
                command.tenantId()
        ));
        boolean duplicated = duplicates.stream().anyMatch(policy -> !policy.id().equals(currentId));
        if (duplicated) {
            throw new BizException(SharedErrorDescriptors.CONFLICT, "Field policy already exists");
        }
    }

    private void ensureSameRowIdentity(
            DataPermission current,
            DataPermissionCommands.SaveRowPolicyCommand command
    ) {
        if (current.subjectType() != command.subjectType()
                || !current.subjectId().equals(command.subjectId())
                || !current.businessObject().equals(command.businessObject())
                || !Objects.equals(current.tenantId(), command.tenantId())) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Row policy identity cannot change");
        }
    }

    private void ensureSameFieldIdentity(
            FieldPermission current,
            DataPermissionCommands.SaveFieldPolicyCommand command
    ) {
        if (current.subjectType() != command.subjectType()
                || !current.subjectId().equals(command.subjectId())
                || !current.businessObject().equals(command.businessObject())
                || !current.usageScenario().equals(command.usageScenario())
                || !current.fieldCode().equals(command.fieldCode())
                || !Objects.equals(current.tenantId(), command.tenantId())) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Field policy identity cannot change");
        }
    }

    private List<SubjectReference> requireSubjects(List<SubjectReference> subjects) {
        if (subjects == null || subjects.isEmpty()) {
            throw new BizException(SharedErrorDescriptors.BAD_REQUEST, "subjects must not be empty");
        }
        return subjects;
    }

    private static int subjectSpecificity(PermissionSubjectType subjectType) {
        return switch (subjectType) {
            case PERSON -> 0;
            case POSITION -> 1;
            case DEPARTMENT -> 2;
            case ORGANIZATION -> 3;
            case ROLE -> 4;
        };
    }

    private String toSqlCondition(DataPermission policy, List<SubjectReference> subjects) {
        if (policy.effect() == PermissionEffect.DENY) {
            return "1 = 0";
        }
        return switch (policy.scopeType()) {
            case ALL -> "1 = 1";
            case SELF -> equalityCondition("person_id", findSubject(subjects, PermissionSubjectType.PERSON));
            case ORG_AND_CHILDREN -> equalityCondition(
                    "organization_id",
                    findSubject(subjects, PermissionSubjectType.ORGANIZATION)
            );
            case DEPT_AND_CHILDREN -> equalityCondition(
                    "department_id",
                    findSubject(subjects, PermissionSubjectType.DEPARTMENT)
            );
            case CONDITION -> substituteCondition(policy.conditionExpr(), subjects);
            case CUSTOM -> substituteCondition(policy.conditionExpr(), subjects);
        };
    }

    private UUID findSubject(List<SubjectReference> subjects, PermissionSubjectType subjectType) {
        return subjects.stream()
                .filter(subject -> subject.subjectType() == subjectType)
                .map(SubjectReference::subjectId)
                .findFirst()
                .orElse(null);
    }

    private String equalityCondition(String column, UUID value) {
        return value == null ? "1 = 0" : column + " = '" + value + "'";
    }

    private String substituteCondition(String conditionExpr, List<SubjectReference> subjects) {
        String condition = DataPermission.normalizeNullable(conditionExpr);
        if (condition == null) {
            return "1 = 0";
        }
        return condition
                .replace("{personId}", String.valueOf(findSubject(subjects, PermissionSubjectType.PERSON)))
                .replace("{positionId}", String.valueOf(findSubject(subjects, PermissionSubjectType.POSITION)))
                .replace("{roleId}", String.valueOf(findSubject(subjects, PermissionSubjectType.ROLE)))
                .replace("{departmentId}", String.valueOf(findSubject(subjects, PermissionSubjectType.DEPARTMENT)))
                .replace("{organizationId}", String.valueOf(findSubject(subjects, PermissionSubjectType.ORGANIZATION)));
    }

    private Instant now() {
        return clock.instant();
    }
}
