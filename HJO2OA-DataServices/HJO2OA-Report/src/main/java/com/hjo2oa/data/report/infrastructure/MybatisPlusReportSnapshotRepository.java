package com.hjo2oa.data.report.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hjo2oa.data.report.domain.ReportFreshnessStatus;
import com.hjo2oa.data.report.domain.ReportRefreshTriggerMode;
import com.hjo2oa.data.report.domain.ReportSnapshot;
import com.hjo2oa.data.report.domain.ReportSnapshotPage;
import com.hjo2oa.data.report.domain.ReportSnapshotPayload;
import com.hjo2oa.data.report.domain.ReportSnapshotRepository;
import com.hjo2oa.shared.tenant.TenantContextHolder;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MybatisPlusReportSnapshotRepository implements ReportSnapshotRepository {

    private final ReportSnapshotMapper reportSnapshotMapper;
    private final ReportDefinitionMapper reportDefinitionMapper;
    private final ReportJsonCodec reportJsonCodec;

    public MybatisPlusReportSnapshotRepository(
            ReportSnapshotMapper reportSnapshotMapper,
            ReportDefinitionMapper reportDefinitionMapper,
            ReportJsonCodec reportJsonCodec
    ) {
        this.reportSnapshotMapper = Objects.requireNonNull(reportSnapshotMapper, "reportSnapshotMapper must not be null");
        this.reportDefinitionMapper = Objects.requireNonNull(
                reportDefinitionMapper,
                "reportDefinitionMapper must not be null");
        this.reportJsonCodec = Objects.requireNonNull(reportJsonCodec, "reportJsonCodec must not be null");
    }

    @Override
    public ReportSnapshot save(ReportSnapshot snapshot) {
        Instant now = Instant.now();
        ReportSnapshotDO snapshotDO = new ReportSnapshotDO()
                .setId(snapshot.id() == null ? UUID.randomUUID().toString() : snapshot.id())
                .setReportId(snapshot.reportId())
                .setTenantId(resolveTenantId(snapshot.reportId()))
                .setSnapshotAt(snapshot.snapshotAt())
                .setRefreshBatch(snapshot.refreshBatch())
                .setScopeSignature(snapshot.scopeSignature())
                .setPayload(reportJsonCodec.write(snapshot.payload()))
                .setFreshnessStatus(snapshot.freshnessStatus().name())
                .setTriggerMode(snapshot.triggerMode().name())
                .setTriggerReason(snapshot.triggerReason())
                .setErrorMessage(snapshot.errorMessage())
                .setDeleted(Boolean.FALSE)
                .setCreatedAt(now)
                .setUpdatedAt(now);
        reportSnapshotMapper.insert(snapshotDO);
        return toDomain(snapshotDO);
    }

    private String resolveTenantId(String reportId) {
        return TenantContextHolder.currentTenantId()
                .map(UUID::toString)
                .orElseGet(() -> {
                    String tenantId = reportDefinitionMapper.selectTenantIdByReportId(reportId);
                    if (tenantId == null || tenantId.isBlank()) {
                        throw new IllegalStateException("report definition tenant not found: " + reportId);
                    }
                    return tenantId;
                });
    }

    @Override
    public Optional<ReportSnapshot> findLatestByReportId(String reportId) {
        QueryWrapper<ReportSnapshotDO> queryWrapper = new QueryWrapper<ReportSnapshotDO>()
                .eq("report_id", reportId)
                .eq("deleted", 0)
                .orderByDesc("snapshot_at");
        return reportSnapshotMapper.selectList(queryWrapper).stream().findFirst().map(this::toDomain);
    }

    @Override
    public Optional<ReportSnapshot> findLatestReadyByReportId(String reportId) {
        QueryWrapper<ReportSnapshotDO> queryWrapper = new QueryWrapper<ReportSnapshotDO>()
                .eq("report_id", reportId)
                .eq("freshness_status", ReportFreshnessStatus.READY.name())
                .eq("deleted", 0)
                .orderByDesc("snapshot_at");
        return reportSnapshotMapper.selectList(queryWrapper).stream().findFirst().map(this::toDomain);
    }

    @Override
    public boolean existsByReportIdAndRefreshBatch(String reportId, String refreshBatch) {
        QueryWrapper<ReportSnapshotDO> queryWrapper = new QueryWrapper<ReportSnapshotDO>()
                .eq("report_id", reportId)
                .eq("refresh_batch", refreshBatch)
                .eq("deleted", 0);
        return reportSnapshotMapper.selectCount(queryWrapper) > 0;
    }

    @Override
    public ReportSnapshotPage pageByReportId(String reportId, int page, int size) {
        QueryWrapper<ReportSnapshotDO> queryWrapper = new QueryWrapper<ReportSnapshotDO>()
                .eq("report_id", reportId)
                .eq("deleted", 0)
                .orderByDesc("snapshot_at");
        List<ReportSnapshotDO> allItems = reportSnapshotMapper.selectList(queryWrapper);
        int fromIndex = Math.min((page - 1) * size, allItems.size());
        int toIndex = Math.min(fromIndex + size, allItems.size());
        List<ReportSnapshot> items = allItems.subList(fromIndex, toIndex).stream().map(this::toDomain).toList();
        return new ReportSnapshotPage(items, allItems.size());
    }

    private ReportSnapshot toDomain(ReportSnapshotDO snapshotDO) {
        return new ReportSnapshot(
                snapshotDO.getId(),
                snapshotDO.getReportId(),
                snapshotDO.getSnapshotAt(),
                snapshotDO.getRefreshBatch(),
                snapshotDO.getScopeSignature(),
                reportJsonCodec.read(snapshotDO.getPayload(), ReportSnapshotPayload.class),
                ReportFreshnessStatus.valueOf(snapshotDO.getFreshnessStatus()),
                ReportRefreshTriggerMode.valueOf(snapshotDO.getTriggerMode()),
                snapshotDO.getTriggerReason(),
                snapshotDO.getErrorMessage()
        );
    }
}
