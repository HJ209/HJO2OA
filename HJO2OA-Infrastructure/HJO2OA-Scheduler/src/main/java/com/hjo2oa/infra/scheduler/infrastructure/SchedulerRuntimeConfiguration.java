package com.hjo2oa.infra.scheduler.infrastructure;

import com.hjo2oa.infra.scheduler.application.SchedulerJobLock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulerRuntimeConfiguration {

    @Bean
    @ConditionalOnMissingBean(TaskScheduler.class)
    public ThreadPoolTaskScheduler infraTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix("infra-scheduler-");
        scheduler.setPoolSize(4);
        scheduler.setRemoveOnCancelPolicy(true);
        return scheduler;
    }

    @Bean
    @ConditionalOnMissingBean(SchedulerJobLock.class)
    public SchedulerJobLock infraSchedulerJobLock() {
        return new InMemorySchedulerJobLock();
    }

    @Bean(name = "infraSchedulerHandlerExecutor", destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "infraSchedulerHandlerExecutor")
    public ExecutorService infraSchedulerHandlerExecutor() {
        return Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("infra-scheduler-handler-" + thread.getId());
            thread.setDaemon(true);
            return thread;
        });
    }
}
