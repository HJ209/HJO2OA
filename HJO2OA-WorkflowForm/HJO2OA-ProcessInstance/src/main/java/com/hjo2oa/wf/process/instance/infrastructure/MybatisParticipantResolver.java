package com.hjo2oa.wf.process.instance.infrastructure;

import com.hjo2oa.wf.process.definition.domain.model.WorkflowParticipantRule;
import com.hjo2oa.wf.process.instance.application.ParticipantResolutionContext;
import com.hjo2oa.wf.process.instance.application.ParticipantResolver;
import com.hjo2oa.wf.process.instance.application.ProcessInstanceCommands;
import com.hjo2oa.wf.process.instance.domain.CandidateType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MybatisParticipantResolver implements ParticipantResolver {

    private final OrgParticipantMapper mapper;

    public MybatisParticipantResolver(OrgParticipantMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<ProcessInstanceCommands.TaskParticipantCommand> resolve(
            WorkflowParticipantRule rule,
            ParticipantResolutionContext context
    ) {
        if (rule == null) {
            return List.of();
        }
        String type = normalizeType(rule.type());
        List<UUID> ids = resolveIds(rule, context);
        if ("INITIATOR".equals(type)) {
            return toParticipants(List.of(new OrgParticipantRow(
                    context.initiatorId(),
                    context.initiatorOrgId(),
                    context.initiatorDeptId(),
                    context.initiatorPositionId()
            )), CandidateType.PERSON);
        }
        if (ids.isEmpty()) {
            return List.of();
        }
        return switch (type) {
            case "PERSON", "SPECIFIC_PERSON" -> toParticipants(mapper.findPeople(context.tenantId(), ids), CandidateType.PERSON);
            case "ORG", "ORGANIZATION", "ORGANIZATION_HOLDER" ->
                    toParticipants(mapper.findByOrganizations(context.tenantId(), ids), CandidateType.PERSON);
            case "DEPT", "DEPARTMENT", "DEPT_HOLDER" ->
                    toParticipants(mapper.findByDepartments(context.tenantId(), ids), CandidateType.PERSON);
            case "POSITION", "POSITION_HOLDER" ->
                    toParticipants(mapper.findByPositions(context.tenantId(), ids), CandidateType.POSITION);
            case "ROLE", "ROLE_HOLDER" -> toParticipants(mapper.findByRoles(context.tenantId(), ids), CandidateType.ROLE);
            case "DEPT_MANAGER" -> toParticipants(mapper.findDepartmentManagers(context.tenantId(), ids), CandidateType.DEPT_MANAGER);
            default -> List.of();
        };
    }

    private List<ProcessInstanceCommands.TaskParticipantCommand> toParticipants(
            List<OrgParticipantRow> rows,
            CandidateType candidateType
    ) {
        Map<UUID, OrgParticipantRow> unique = new LinkedHashMap<>();
        for (OrgParticipantRow row : rows) {
            if (row.personId() != null) {
                unique.putIfAbsent(row.personId(), row);
            }
        }
        if (unique.isEmpty()) {
            return List.of();
        }
        List<UUID> candidateIds = unique.keySet().stream().toList();
        if (candidateIds.size() == 1) {
            OrgParticipantRow only = unique.values().iterator().next();
            return List.of(new ProcessInstanceCommands.TaskParticipantCommand(
                    only.personId(),
                    only.orgId(),
                    only.deptId(),
                    only.positionId(),
                    candidateType,
                    candidateIds
            ));
        }
        return List.of(new ProcessInstanceCommands.TaskParticipantCommand(
                null,
                null,
                null,
                null,
                candidateType,
                candidateIds
        ));
    }

    private List<UUID> resolveIds(WorkflowParticipantRule rule, ParticipantResolutionContext context) {
        if ("FORM_FIELD_VALUE".equals(normalizeType(rule.type())) && rule.refFieldCode() != null) {
            Object value = context.variables().get(rule.refFieldCode());
            if (value instanceof List<?> list) {
                return list.stream().map(Object::toString).map(this::tryParseUuid).filter(id -> id != null).toList();
            }
            UUID id = value == null ? null : tryParseUuid(value.toString());
            return id == null ? List.of() : List.of(id);
        }
        return rule.ids().stream().map(this::tryParseUuid).filter(id -> id != null).toList();
    }

    private UUID tryParseUuid(String value) {
        try {
            return value == null || value.isBlank() ? null : UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String normalizeType(String type) {
        return type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
    }
}
