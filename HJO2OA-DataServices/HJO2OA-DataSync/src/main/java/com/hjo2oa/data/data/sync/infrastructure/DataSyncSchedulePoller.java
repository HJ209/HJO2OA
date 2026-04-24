package com.hjo2oa.data.data.sync.infrastructure;

import com.hjo2oa.data.data.sync.application.SyncExecutionApplicationService;
import com.hjo2oa.data.data.sync.domain.SyncExchangeTask;
import com.hjo2oa.data.data.sync.domain.SyncExchangeTaskRepository;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

@Component
public class DataSyncSchedulePoller {

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
            ZonedDateTime previousWindow = now.minusNanos(1);
            ZonedDateTime scheduledAt = cronExpression.next(previousWindow);
            if (scheduledAt == null || scheduledAt.isAfter(now)) {
                continue;
            }
            Map<String, Object> triggerContext = new LinkedHashMap<>();
            triggerContext.put("triggerAt", scheduledAt.toInstant().toString());
            if (task.scheduleConfig().schedulerJobCode() != null) {
                triggerContext.put("jobCode", task.scheduleConfig().schedulerJobCode());
            }
            executionApplicationService.triggerScheduledTask(
                    task,
                    "cron:" + task.taskId() + ":" + scheduledAt.toInstant().toEpochMilli(),
                    triggerContext
            );
        }
    }
}
