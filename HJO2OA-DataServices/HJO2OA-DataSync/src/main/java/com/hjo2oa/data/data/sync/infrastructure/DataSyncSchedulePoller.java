package com.hjo2oa.data.data.sync.infrastructure;

import com.hjo2oa.data.data.sync.application.SyncExecutionApplicationService;
import com.hjo2oa.data.data.sync.domain.SyncExchangeTask;
import com.hjo2oa.data.data.sync.domain.SyncExchangeTaskRepository;
import com.hjo2oa.shared.tenant.TenantContextHolder;
import com.hjo2oa.shared.tenant.TenantRequestContext;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

@Component
public class DataSyncSchedulePoller {

    private static final Duration SCHEDULE_LOOKBACK = Duration.ofMinutes(2);

    private final SyncExchangeTaskRepository taskRepository;
    private final SyncExecutionApplicationService executionApplicationService;
    private final Clock clock;

    public DataSyncSchedulePoller(
            SyncExchangeTaskRepository taskRepository,
            SyncExecutionApplicationService executionApplicationService,
            Clock clock
    ) {
        this.taskRepository = taskRepository;
        this.executionApplicationService = executionApplicationService;
        this.clock = clock;
    }

    @Scheduled(cron = "${hjo2oa.data-sync.scheduler.scan-cron:0 * * * * *}")
    public void scanAndTrigger() {
        for (SyncExchangeTask task : taskRepository.findActiveScheduledTasks()) {
            if (!task.scheduleConfig().enabled() || task.scheduleConfig().cron() == null) {
                continue;
            }
            ZoneId zoneId = task.scheduleConfig().zoneId() == null
                    ? ZoneId.of("UTC")
                    : ZoneId.of(task.scheduleConfig().zoneId());
            ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(zoneId);
            CronExpression cronExpression = CronExpression.parse(task.scheduleConfig().cron());
            ZonedDateTime scheduledAt = latestScheduledAt(cronExpression, now);
            if (scheduledAt == null) {
                continue;
            }
            Map<String, Object> triggerContext = new LinkedHashMap<>();
            triggerContext.put("triggerAt", scheduledAt.toInstant().toString());
            if (task.scheduleConfig().schedulerJobCode() != null) {
                triggerContext.put("jobCode", task.scheduleConfig().schedulerJobCode());
            }
            String requestId = "cron:" + task.taskId() + ":" + scheduledAt.toInstant().toEpochMilli();
            TenantRequestContext tenantContext = TenantRequestContext.builder()
                    .tenantId(task.tenantId())
                    .requestId(requestId)
                    .timezone(zoneId.getId())
                    .build();
            try (TenantContextHolder.Scope ignored = TenantContextHolder.bind(tenantContext)) {
                executionApplicationService.triggerScheduledTask(task, requestId, triggerContext);
            }
        }
    }

    private ZonedDateTime latestScheduledAt(CronExpression cronExpression, ZonedDateTime now) {
        ZonedDateTime cursor = now.minus(SCHEDULE_LOOKBACK);
        ZonedDateTime latest = null;
        while (true) {
            ZonedDateTime next = cronExpression.next(cursor);
            if (next == null || next.isAfter(now)) {
                return latest;
            }
            latest = next;
            cursor = next;
        }
    }
}
