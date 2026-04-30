package com.hjo2oa.shared.tenant;

import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;

@Component
public class TenantContextTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        return TenantContextHolder.wrap(runnable);
    }
}
