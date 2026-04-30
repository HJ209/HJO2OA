package com.hjo2oa.wf.process.definition.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.wf.process.definition.domain.ActionCategory;
import com.hjo2oa.wf.process.definition.domain.DefinitionStatus;
import com.hjo2oa.wf.process.definition.domain.ProcessDefinitionView;
import com.hjo2oa.wf.process.definition.domain.RouteTarget;
import com.hjo2oa.wf.process.definition.infrastructure.InMemoryActionDefinitionRepository;
import com.hjo2oa.wf.process.definition.infrastructure.InMemoryProcessDefinitionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProcessDefinitionApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T06:00:00Z");
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID FORM_METADATA_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void shouldVersionPublishAndDeprecatePreviousActiveDefinition() {
        ProcessDefinitionApplicationService service = service();

        ProcessDefinitionView first = service.createDefinition(definitionCommand(null, "Leave Approval"));
        ProcessDefinitionView activeFirst =
                service.publishDefinition(new ProcessDefinitionCommands.PublishDefinitionCommand(
                        first.id(),
                        null,
                        "publish-first",
                        "req-publish-first"
                ));
        ProcessDefinitionView second = service.createNextVersion(activeFirst.id());

        assertThat(activeFirst.status()).isEqualTo(DefinitionStatus.PUBLISHED);
        assertThat(second.version()).isEqualTo(2);
        assertThat(second.status()).isEqualTo(DefinitionStatus.DRAFT);

        ProcessDefinitionView activeSecond =
                service.publishDefinition(new ProcessDefinitionCommands.PublishDefinitionCommand(
                        second.id(),
                        null,
                        "publish-second",
                        "req-publish-second"
                ));
        List<ProcessDefinitionView> versions = service.queryDefinitions(new ProcessDefinitionCommands.DefinitionQuery(
                TENANT_ID,
                "leave",
                null,
                null
        ));

        assertThat(activeSecond.status()).isEqualTo(DefinitionStatus.PUBLISHED);
        assertThat(versions)
                .extracting(ProcessDefinitionView::status)
                .containsExactly(DefinitionStatus.PUBLISHED, DefinitionStatus.DEPRECATED);
    }

    @Test
    void shouldConfigureActionDefinitions() {
        ProcessDefinitionApplicationService service = service();

        UUID actionId = service.createAction(new ProcessDefinitionCommands.SaveActionCommand(
                null,
                "approve",
                "Approve",
                ActionCategory.APPROVE,
                RouteTarget.NEXT_NODE,
                false,
                false,
                "{\"color\":\"green\"}",
                TENANT_ID,
                "action-create",
                "req-action-create"
        )).id();

        service.updateAction(new ProcessDefinitionCommands.SaveActionCommand(
                actionId,
                "approve",
                "Approve Updated",
                ActionCategory.APPROVE,
                RouteTarget.NEXT_NODE,
                true,
                false,
                "{\"color\":\"blue\"}",
                TENANT_ID,
                "action-update",
                "req-action-update"
        ));

        assertThat(service.getAction(actionId).name()).isEqualTo("Approve Updated");
        assertThat(service.queryActions(new ProcessDefinitionCommands.ActionQuery(TENANT_ID, ActionCategory.APPROVE)))
                .hasSize(1);

        service.deleteAction(actionId);

        assertThat(service.queryActions(new ProcessDefinitionCommands.ActionQuery(TENANT_ID, null))).isEmpty();
    }

    private ProcessDefinitionCommands.SaveDefinitionCommand definitionCommand(UUID definitionId, String name) {
        return new ProcessDefinitionCommands.SaveDefinitionCommand(
                definitionId,
                "leave",
                name,
                "HR",
                FORM_METADATA_ID,
                "start",
                "end",
                """
                        [
                          {"nodeId":"start","type":"START","name":"Start"},
                          {"nodeId":"approve","type":"USER_TASK","name":"Approve",
                           "participantRule":{"type":"SPECIFIC_PERSON","ids":["person-1"]},
                           "actionCodes":["approve"]},
                          {"nodeId":"end","type":"END","name":"End"}
                        ]
                        """,
                """
                        [
                          {"routeId":"r1","sourceNodeId":"start","targetNodeId":"approve"},
                          {"routeId":"r2","sourceNodeId":"approve","targetNodeId":"end"}
                        ]
                        """,
                TENANT_ID,
                "definition-" + name,
                "req-definition-" + name
        );
    }

    private ProcessDefinitionApplicationService service() {
        return new ProcessDefinitionApplicationService(
                new InMemoryProcessDefinitionRepository(),
                new InMemoryActionDefinitionRepository(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }
}
