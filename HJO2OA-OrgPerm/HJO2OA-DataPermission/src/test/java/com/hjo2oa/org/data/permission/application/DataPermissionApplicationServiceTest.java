package com.hjo2oa.org.data.permission.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hjo2oa.org.data.permission.domain.DataPermissionDecisionView;
import com.hjo2oa.org.data.permission.domain.DataPermissionQuery;
import com.hjo2oa.org.data.permission.domain.DataScopeType;
import com.hjo2oa.org.data.permission.domain.FieldPermissionAction;
import com.hjo2oa.org.data.permission.domain.FieldPermissionDecisionView;
import com.hjo2oa.org.data.permission.domain.FieldPermissionRuntimeMasker;
import com.hjo2oa.org.data.permission.domain.PermissionEffect;
import com.hjo2oa.org.data.permission.domain.PermissionSubjectType;
import com.hjo2oa.org.data.permission.domain.SubjectReference;
import com.hjo2oa.org.data.permission.infrastructure.InMemoryDataPermissionRepository;
import com.hjo2oa.shared.kernel.BizException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DataPermissionApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-27T02:00:00Z");

    @Test
    void shouldPreferDenyAndMoreSpecificSubjectsForRowDecision() {
        DataPermissionApplicationService service = newService();
        UUID roleId = UUID.randomUUID();
        UUID personId = UUID.randomUUID();

        service.createRowPolicy(new DataPermissionCommands.SaveRowPolicyCommand(
                PermissionSubjectType.ROLE,
                roleId,
                "process_instance",
                DataScopeType.ALL,
                null,
                PermissionEffect.ALLOW,
                100,
                null
        ));
        service.createRowPolicy(new DataPermissionCommands.SaveRowPolicyCommand(
                PermissionSubjectType.PERSON,
                personId,
                "process_instance",
                DataScopeType.SELF,
                null,
                PermissionEffect.DENY,
                0,
                null
        ));

        DataPermissionDecisionView decision = service.decideRow(new DataPermissionCommands.RowDecisionQuery(
                "process_instance",
                null,
                List.of(
                        new SubjectReference(PermissionSubjectType.ROLE, roleId),
                        new SubjectReference(PermissionSubjectType.PERSON, personId)
                )
        ));

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.effect()).isEqualTo(PermissionEffect.DENY);
        assertThat(decision.sqlCondition()).isEqualTo("1 = 0");
        assertThat(decision.scopeType()).isEqualTo(DataScopeType.SELF);
        assertThat(decision.matchedPolicies()).hasSize(2);
    }

    @Test
    void shouldSupportCustomConditionAndRejectMissingCondition() {
        DataPermissionApplicationService service = newService();
        UUID roleId = UUID.randomUUID();
        UUID personId = UUID.randomUUID();

        service.createRowPolicy(new DataPermissionCommands.SaveRowPolicyCommand(
                PermissionSubjectType.ROLE,
                roleId,
                "content_article",
                DataScopeType.CUSTOM,
                "owner_person_id = '{personId}' OR reviewer_role_id = '{roleId}'",
                PermissionEffect.ALLOW,
                0,
                null
        ));

        DataPermissionDecisionView decision = service.decideRow(new DataPermissionCommands.RowDecisionQuery(
                "content_article",
                null,
                List.of(
                        new SubjectReference(PermissionSubjectType.ROLE, roleId),
                        new SubjectReference(PermissionSubjectType.PERSON, personId)
                )
        ));

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.scopeType()).isEqualTo(DataScopeType.CUSTOM);
        assertThat(decision.sqlCondition()).contains(personId.toString());

        assertThatThrownBy(() -> service.createRowPolicy(new DataPermissionCommands.SaveRowPolicyCommand(
                PermissionSubjectType.ROLE,
                UUID.randomUUID(),
                "content_article",
                DataScopeType.CONDITION,
                " ",
                PermissionEffect.ALLOW,
                0,
                null
        ))).isInstanceOf(BizException.class);
    }

    @Test
    void shouldCreateFieldMatrixAndPreventDuplicates() {
        DataPermissionApplicationService service = newService();
        UUID roleId = UUID.randomUUID();

        service.createFieldPolicy(new DataPermissionCommands.SaveFieldPolicyCommand(
                PermissionSubjectType.ROLE,
                roleId,
                "person_profile",
                "view",
                "mobile",
                FieldPermissionAction.DESENSITIZED,
                PermissionEffect.ALLOW,
                null
        ));

        assertThatThrownBy(() -> service.createFieldPolicy(new DataPermissionCommands.SaveFieldPolicyCommand(
                PermissionSubjectType.ROLE,
                roleId,
                "person_profile",
                "view",
                "mobile",
                FieldPermissionAction.DESENSITIZED,
                PermissionEffect.ALLOW,
                null
        ))).isInstanceOf(BizException.class);

        FieldPermissionDecisionView decision = service.decideField(new DataPermissionCommands.FieldDecisionQuery(
                "person_profile",
                "view",
                null,
                List.of("mobile"),
                List.of(new SubjectReference(PermissionSubjectType.ROLE, roleId))
        ));

        assertThat(decision.fieldEffects())
                .containsKey("mobile")
                .extractingByKey("mobile")
                .satisfies(actions -> assertThat(actions)
                        .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                        .containsEntry(FieldPermissionAction.DESENSITIZED, PermissionEffect.ALLOW));
        assertThat(decision.desensitizedFields()).containsExactly("mobile");
        assertThat(new FieldPermissionRuntimeMasker().apply(decision, Map.of("mobile", "13812345678")))
                .containsEntry("mobile", "138****78");
    }

    @Test
    void shouldQueryAndDeleteRowPolicies() {
        DataPermissionApplicationService service = newService();
        UUID positionId = UUID.randomUUID();
        UUID policyId = service.createRowPolicy(new DataPermissionCommands.SaveRowPolicyCommand(
                PermissionSubjectType.POSITION,
                positionId,
                "report_snapshot",
                DataScopeType.CONDITION,
                "department_id = current_department_id",
                PermissionEffect.ALLOW,
                10,
                null
        )).id();

        assertThat(service.queryRowPolicies(new DataPermissionQuery(
                PermissionSubjectType.POSITION,
                positionId,
                "report_snapshot",
                null,
                null,
                null
        ))).hasSize(1);

        service.deleteRowPolicy(policyId);

        assertThat(service.findRowPolicy(policyId)).isEmpty();
    }

    private DataPermissionApplicationService newService() {
        return new DataPermissionApplicationService(
                new InMemoryDataPermissionRepository(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }
}
