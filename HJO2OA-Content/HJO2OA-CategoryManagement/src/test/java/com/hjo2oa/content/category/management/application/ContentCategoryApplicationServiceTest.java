package com.hjo2oa.content.category.management.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.content.category.management.application.ContentCategoryApplicationService.CategoryStatus;
import com.hjo2oa.content.category.management.application.ContentCategoryApplicationService.CategoryTreeNode;
import com.hjo2oa.content.category.management.application.ContentCategoryApplicationService.CategoryView;
import com.hjo2oa.content.category.management.application.ContentCategoryApplicationService.CreateCategoryCommand;
import com.hjo2oa.content.category.management.application.ContentCategoryApplicationService.MoveCategoryCommand;
import com.hjo2oa.content.category.management.application.ContentCategoryApplicationService.PermissionEffect;
import com.hjo2oa.content.category.management.application.ContentCategoryApplicationService.PermissionPreviewResult;
import com.hjo2oa.content.category.management.application.ContentCategoryApplicationService.PermissionRuleInput;
import com.hjo2oa.content.category.management.application.ContentCategoryApplicationService.PermissionScope;
import com.hjo2oa.content.category.management.application.ContentCategoryApplicationService.PermissionSubjectContext;
import com.hjo2oa.content.category.management.application.ContentCategoryApplicationService.PermissionSubjectType;
import com.hjo2oa.content.category.management.infrastructure.InMemoryContentCategoryRepository;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ContentCategoryApplicationServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OPERATOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final Instant NOW = Instant.parse("2026-04-20T10:00:00Z");

    @Test
    void shouldBuildTreeSortMoveDisableAndEvaluatePermissions() {
        List<DomainEvent> events = new ArrayList<>();
        ContentCategoryApplicationService service = new ContentCategoryApplicationService(
                new InMemoryContentCategoryRepository(),
                events::add,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        CategoryView root = service.create(createCommand("news", "新闻", null, 10, List.of(
                new PermissionRuleInput(PermissionSubjectType.ALL, null, PermissionEffect.ALLOW, PermissionScope.MANAGE, 0)
        )));
        CategoryView firstChild = service.create(createCommand("policy", "制度", root.id(), 20, null));
        CategoryView secondChild = service.create(createCommand("notice", "公告", root.id(), 10, null));

        List<CategoryTreeNode> tree = service.tree(TENANT_ID, true);

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).category().id()).isEqualTo(root.id());
        assertThat(tree.get(0).children()).extracting(node -> node.category().code())
                .containsExactly("notice", "policy");

        service.move(firstChild.id(), new MoveCategoryCommand(TENANT_ID, OPERATOR_ID, root.id(), 5, firstChild.versionNo(), "idem-1"));
        int afterMoveEvents = events.size();
        service.move(firstChild.id(), new MoveCategoryCommand(TENANT_ID, OPERATOR_ID, root.id(), 5, firstChild.versionNo() + 1, "idem-1"));
        assertThat(events).hasSize(afterMoveEvents);

        service.disable(TENANT_ID, secondChild.id(), OPERATOR_ID);
        int afterDisableEvents = events.size();
        service.disable(TENANT_ID, secondChild.id(), OPERATOR_ID);
        assertThat(events).hasSize(afterDisableEvents);

        List<CategoryTreeNode> enabledOnly = service.tree(TENANT_ID, true);
        assertThat(enabledOnly.get(0).children()).extracting(node -> node.category().code())
                .containsExactly("policy");
        assertThat(service.get(TENANT_ID, secondChild.id()).status()).isEqualTo(CategoryStatus.DISABLED);

        PermissionPreviewResult decision = service.previewPermission(
                TENANT_ID,
                root.id(),
                new PermissionSubjectContext(OPERATOR_ID, null, null, null, Set.of()),
                PermissionScope.MANAGE
        );
        assertThat(decision.allowed()).isTrue();
        assertThat(events).extracting(DomainEvent::eventType)
                .contains("content.category.created", "content.category.moved", "content.category.disabled");
    }

    private static CreateCategoryCommand createCommand(
            String code,
            String name,
            UUID parentId,
            int sortOrder,
            List<PermissionRuleInput> permissions
    ) {
        return new CreateCategoryCommand(
                TENANT_ID,
                OPERATOR_ID,
                code,
                name,
                "GENERAL",
                parentId,
                "/" + code,
                sortOrder,
                "INHERIT",
                permissions,
                null
        );
    }
}
