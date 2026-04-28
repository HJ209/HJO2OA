package com.hjo2oa.msg.message.center.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hjo2oa.msg.message.center.domain.Notification;
import com.hjo2oa.msg.message.center.domain.NotificationCategory;
import com.hjo2oa.msg.message.center.domain.NotificationInboxStatus;
import com.hjo2oa.msg.message.center.domain.NotificationPriority;
import com.hjo2oa.msg.message.center.domain.NotificationRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MybatisNotificationRepository implements NotificationRepository {

    private final NotificationMapper mapper;

    public MybatisNotificationRepository(NotificationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<Notification> findByNotificationId(String notificationId) {
        return Optional.ofNullable(mapper.selectById(notificationId)).map(this::toDomain);
    }

    @Override
    public Optional<Notification> findByDedupKey(String dedupKey) {
        return Optional.ofNullable(mapper.selectOne(new QueryWrapper<NotificationEntity>()
                .eq("dedup_key", dedupKey))).map(this::toDomain);
    }

    @Override
    public Notification save(Notification notification) {
        NotificationEntity existing = mapper.selectById(notification.notificationId());
        NotificationEntity entity = toEntity(notification, existing);
        if (existing == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return findByNotificationId(notification.notificationId()).orElseThrow();
    }

    @Override
    public List<Notification> findAll() {
        return mapper.selectList(new QueryWrapper<NotificationEntity>().orderByDesc("created_at"))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private Notification toDomain(NotificationEntity entity) {
        return new Notification(
                entity.getNotificationId(),
                entity.getDedupKey(),
                entity.getTenantId(),
                entity.getRecipientId(),
                entity.getTargetAssignmentId(),
                entity.getTargetPositionId(),
                entity.getTitle(),
                entity.getBodySummary(),
                entity.getDeepLink(),
                NotificationCategory.valueOf(entity.getCategory()),
                NotificationPriority.valueOf(entity.getPriority()),
                NotificationInboxStatus.valueOf(entity.getInboxStatus()),
                entity.getSourceModule(),
                entity.getSourceEventType(),
                entity.getSourceBusinessId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getReadAt(),
                entity.getArchivedAt(),
                entity.getRevokedAt(),
                entity.getExpiredAt(),
                entity.getStatusReason()
        );
    }

    private NotificationEntity toEntity(Notification notification, NotificationEntity existing) {
        NotificationEntity entity = existing == null ? new NotificationEntity() : existing;
        entity.setNotificationId(notification.notificationId());
        entity.setDedupKey(notification.dedupKey());
        entity.setTenantId(notification.tenantId());
        entity.setRecipientId(notification.recipientId());
        entity.setTargetAssignmentId(notification.targetAssignmentId());
        entity.setTargetPositionId(notification.targetPositionId());
        entity.setTitle(notification.title());
        entity.setBodySummary(notification.bodySummary());
        entity.setDeepLink(notification.deepLink());
        entity.setCategory(notification.category().name());
        entity.setPriority(notification.priority().name());
        entity.setInboxStatus(notification.inboxStatus().name());
        entity.setSourceModule(notification.sourceModule());
        entity.setSourceEventType(notification.sourceEventType());
        entity.setSourceBusinessId(notification.sourceBusinessId());
        entity.setCreatedAt(notification.createdAt());
        entity.setUpdatedAt(notification.updatedAt());
        entity.setReadAt(notification.readAt());
        entity.setArchivedAt(notification.archivedAt());
        entity.setRevokedAt(notification.revokedAt());
        entity.setExpiredAt(notification.expiredAt());
        entity.setStatusReason(notification.statusReason());
        return entity;
    }
}
