package com.hjo2oa.data.report.domain;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hjo2oa.data.report.support.ReportTestSupport;
import org.junit.jupiter.api.Test;

class ReportDefinitionTest {

    @Test
    void shouldRejectPortalCardDefinitionWithoutMetrics() {
        assertThrows(IllegalArgumentException.class, () -> ReportDefinition.draft(
                "card-without-metrics",
                "空图卡",
                ReportType.CARD,
                ReportSourceScope.TASK,
                ReportRefreshMode.ON_DEMAND,
                ReportVisibilityMode.PORTAL_CARD,
                ReportTestSupport.TENANT_ID,
                new ReportCaliberDefinition(
                        ReportTestSupport.PROVIDER_KEY,
                        "TASK",
                        "occurredAt",
                        "organizationCode",
                        null,
                        null,
                        null,
                        null
                ),
                new ReportRefreshConfig(null, null, null),
                new ReportCardProtocol("empty-card", "空图卡", ReportCardType.SUMMARY, "volume", null, null, null, 5),
                java.util.List.of(),
                java.util.List.of(),
                ReportTestSupport.FIXED_TIME
        ));
    }
}
