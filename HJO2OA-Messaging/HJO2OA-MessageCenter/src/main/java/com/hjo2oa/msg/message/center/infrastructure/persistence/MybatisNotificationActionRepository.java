package com.hjo2oa.msg.message.center.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hjo2oa.msg.message.center.domain.NotificationAction;
import com.hjo2oa.msg.message.center.domain.NotificationActionRepository;
import com.hjo2oa.msg.message.center.domain.NotificationActionType;
import java.util.List;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MybatisNotificationActionRepository implements NotificationActionRepository {

    private final NotificationActionMapper mapper;

    public MybatisNotificationActionRepository(NotificationActionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public NotificationAction save(NotificationAction action) {
        NotificationActionEntity existing = mapper.selectById(action.actionId());
        NotificationActionEntity entity = existing == null ? new NotificationActionEntity() : existing;
        entity.setActionId(action.actionId());
        entity.setNotificationId(action.notificationId());
        entity.setActionType(action.actionType().name());
        entity.setOperatorId(action.operatorId());
        entity.setOccurredAt(action.occurredAt());
        if (existing == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return action;
    }

    @Override
    public List<NotificationAction> findByNotificationId(String notificationId) {
        return mapper.selectList(new QueryWrapper<NotificationActionEntity>()
                        .eq("notification_id", notificationId)
                        .orderByAsc("occurred_at"))
                .stream()
                .map(entity -> new NotificationAction(
                        entity.getActionId(),
                        entity.getNotificationId(),
                        NotificationActionType.valueOf(entity.getActionType()),
                        entity.getOperatorId(),
                        entity.getOccurredAt()))
                .toList();
    }
}
