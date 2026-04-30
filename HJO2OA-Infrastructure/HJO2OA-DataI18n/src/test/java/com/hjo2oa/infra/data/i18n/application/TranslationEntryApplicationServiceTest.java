package com.hjo2oa.infra.data.i18n.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hjo2oa.infra.data.i18n.domain.TranslationResolveSource;
import com.hjo2oa.infra.data.i18n.domain.TranslationResolutionView;
import com.hjo2oa.infra.data.i18n.domain.TranslationStatus;
import com.hjo2oa.infra.data.i18n.infrastructure.InMemoryTranslationEntryRepository;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TranslationEntryApplicationServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_TENANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T06:00:00Z");

    @Test
    void shouldCreateUpdateReviewAndResolveTranslationWithFallback() {
        TranslationEntryApplicationService applicationService = applicationService();

        var created = applicationService.createTranslation(
                "article",
                "A-100",
                "title",
                "zh-CN",
                "中文标题",
                TENANT_ID
        );
        var updated = applicationService.updateTranslation(created.id(), "中文标题-已更新");
        var reviewed = applicationService.reviewTranslation(created.id());
        TranslationResolutionView resolved = applicationService.resolveTranslation(
                new TranslationEntryCommands.ResolveCommand(
                        "article",
                        "A-100",
                        "title",
                        "en-US",
                        TENANT_ID,
                        "zh-CN",
                        "Article Title"
                )
        );

        assertThat(created.translationStatus()).isEqualTo(TranslationStatus.TRANSLATED);
        assertThat(updated.translatedValue()).isEqualTo("中文标题-已更新");
        assertThat(reviewed.translationStatus()).isEqualTo(TranslationStatus.REVIEWED);
        assertThat(resolved.resolveSource()).isEqualTo(TranslationResolveSource.FALLBACK);
        assertThat(resolved.resolvedLocale()).isEqualTo("zh-CN");
        assertThat(resolved.resolvedValue()).isEqualTo("中文标题-已更新");
    }

    @Test
    void shouldFallbackToOriginalValueWhenTranslationMissing() {
        TranslationEntryApplicationService applicationService = applicationService();

        TranslationResolutionView resolved = applicationService.resolveTranslation(
                new TranslationEntryCommands.ResolveCommand(
                        "article",
                        "A-200",
                        "summary",
                        "en-US",
                        TENANT_ID,
                        "zh-CN",
                        "Original Summary"
                )
        );

        assertThat(resolved.resolveSource()).isEqualTo(TranslationResolveSource.ORIGINAL_VALUE);
        assertThat(resolved.fallbackApplied()).isTrue();
        assertThat(resolved.resolvedLocale()).isNull();
        assertThat(resolved.resolvedValue()).isEqualTo("Original Summary");
    }

    @Test
    void shouldUpsertExistingTranslationDuringBatchSave() {
        TranslationEntryApplicationService applicationService = applicationService();
        applicationService.createTranslation(
                "article",
                "A-300",
                "title",
                "zh-CN",
                "旧标题",
                TENANT_ID
        );

        var saved = applicationService.batchSaveTranslations(List.of(
                new TranslationEntryCommands.BatchSaveItemCommand(
                        null,
                        "article",
                        "A-300",
                        "title",
                        "zh-CN",
                        "批量更新标题",
                        TENANT_ID,
                        null
                ),
                new TranslationEntryCommands.BatchSaveItemCommand(
                        null,
                        "article",
                        "A-300",
                        "summary",
                        "zh-CN",
                        "批量新增摘要",
                        TENANT_ID,
                        null
                )
        ));

        assertThat(saved).hasSize(2);
        assertThat(applicationService.queryByEntity("article", "A-300"))
                .extracting(view -> view.fieldName() + ":" + view.translatedValue())
                .containsExactly("summary:批量新增摘要", "title:批量更新标题");
    }

    @Test
    void shouldRejectDuplicateBusinessKeysInBatch() {
        TranslationEntryApplicationService applicationService = applicationService();

        assertThatThrownBy(() -> applicationService.batchSaveTranslations(List.of(
                new TranslationEntryCommands.BatchSaveItemCommand(
                        null,
                        "article",
                        "A-400",
                        "title",
                        "zh-CN",
                        "标题一",
                        TENANT_ID,
                        null
                ),
                new TranslationEntryCommands.BatchSaveItemCommand(
                        null,
                        "article",
                        "A-400",
                        "title",
                        "zh-CN",
                        "标题二",
                        TENANT_ID,
                        null
                )
        )))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).errorCode())
                        .isEqualTo(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION.code()));
    }

    @Test
    void shouldIsolateSameBusinessKeyByTenantAndInvalidateResolveCache() {
        TranslationEntryApplicationService applicationService = applicationService();
        var tenantTranslation = applicationService.createTranslation(
                "article",
                "A-tenant",
                "title",
                "en-US",
                "Tenant A title",
                TENANT_ID
        );
        applicationService.createTranslation(
                "article",
                "A-tenant",
                "title",
                "en-US",
                "Tenant B title",
                OTHER_TENANT_ID
        );

        assertThat(resolveTitle(applicationService, TENANT_ID)).isEqualTo("Tenant A title");
        assertThat(resolveTitle(applicationService, OTHER_TENANT_ID)).isEqualTo("Tenant B title");

        applicationService.updateTranslation(tenantTranslation.id(), "Tenant A title updated");

        assertThat(resolveTitle(applicationService, TENANT_ID)).isEqualTo("Tenant A title updated");
        assertThat(resolveTitle(applicationService, OTHER_TENANT_ID)).isEqualTo("Tenant B title");
        assertThat(applicationService.listTranslations(TENANT_ID, "article", "en-US"))
                .singleElement()
                .extracting(view -> view.tenantId(), view -> view.translatedValue())
                .containsExactly(TENANT_ID, "Tenant A title updated");
    }

    private String resolveTitle(TranslationEntryApplicationService applicationService, UUID tenantId) {
        return applicationService.resolveTranslation(new TranslationEntryCommands.ResolveCommand(
                "article",
                "A-tenant",
                "title",
                "en-US",
                tenantId,
                null,
                "Original"
        )).resolvedValue();
    }

    private TranslationEntryApplicationService applicationService() {
        return new TranslationEntryApplicationService(
                new InMemoryTranslationEntryRepository(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }
}
