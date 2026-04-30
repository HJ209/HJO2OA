package com.hjo2oa.wf.action.engine.application;

import com.hjo2oa.wf.action.engine.domain.TaskInstanceSnapshot;

public interface TaskActionPolicy {

    boolean isAllowed(TaskInstanceSnapshot task, String actionCode);

    static TaskActionPolicy allowAll() {
        return AllowAllTaskActionPolicy.INSTANCE;
    }

    enum AllowAllTaskActionPolicy implements TaskActionPolicy {
        INSTANCE;

        @Override
        public boolean isAllowed(TaskInstanceSnapshot task, String actionCode) {
            return true;
        }
    }
}
