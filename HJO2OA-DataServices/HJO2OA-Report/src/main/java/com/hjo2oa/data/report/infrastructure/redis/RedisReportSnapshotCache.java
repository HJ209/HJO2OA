package com.hjo2oa.data.report.infrastructure.redis;

import com.hjo2oa.data.report.domain.ReportSnapshot;
import com.hjo2oa.data.report.domain.ReportSnapshotCache;
import com.hjo2oa.shared.cache.RedisCacheOperations;
import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@ConditionalOnProperty(name = "hjo2oa.cache.type", havingValue = "redis")
public class RedisReportSnapshotCache implements ReportSnapshotCache {

    private static final String KEY_PREFIX = "hjo2oa:report:snapshot";

    private final RedisCacheOperations redisCacheOperations;
    private final Duration snapshotTtl;

    public RedisReportSnapshotCache(
            RedisCacheOperations redisCacheOperations,
            @Value("${hjo2oa.data-services.report.snapshot-cache-ttl-seconds:900}") long snapshotTtlSeconds
    ) {
        this.redisCacheOperations = redisCacheOperations;
        this.snapshotTtl = Duration.ofSeconds(snapshotTtlSeconds);
    }

    @Override
    public Optional<ReportSnapshot> findReadySnapshotByCode(String reportCode) {
        return redisCacheOperations.get(snapshotKey(reportCode), ReportSnapshot.class);
    }

    @Override
    public void put(String reportCode, ReportSnapshot snapshot) {
        redisCacheOperations.set(snapshotKey(reportCode), snapshot, snapshotTtl);
    }

    @Override
    public void invalidate(String reportCode) {
        redisCacheOperations.delete(snapshotKey(reportCode));
    }

    private static String snapshotKey(String reportCode) {
        return KEY_PREFIX + ":" + reportCode;
    }
}
