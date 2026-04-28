package com.hjo2oa.msg.message.center.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hjo2oa.msg.message.center.domain.NotificationDeliveryChannel;
import com.hjo2oa.msg.message.center.domain.NotificationDeliveryRecord;
import com.hjo2oa.msg.message.center.domain.NotificationDeliveryRecordRepository;
import com.hjo2oa.msg.message.center.domain.NotificationDeliveryStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MybatisNotificationDeliveryRecordRepository implements NotificationDeliveryRecordRepository {

    private final NotificationDeliveryRecordMapper mapper;

    public MybatisNotificationDeliveryRecordRepository(NotificationDeliveryRecordMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<NotificationDeliveryRecord> findByNotificationIdAndChannel(
            String notificationId,
            NotificationDeliveryChannel channel
    ) {
        return Optional.ofNullable(mapper.selectOne(new QueryWrapper<NotificationDeliveryRecordEntity>()
                .eq("notification_id", notificationId)
                .eq("channel", channel.name()))).map(this::toDomain);
    }

    @Override
    public NotificationDeliveryRecord save(NotificationDeliveryRecord deliveryRecord) {
        NotificationDeliveryRecordEntity existing = mapper.selectById(deliveryRecord.deliveryId());
        NotificationDeliveryRecordEntity entity = toEntity(deliveryRecord, existing);
        if (existing == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return toDomain(mapper.selectById(deliveryRecord.deliveryId()));
    }

    @Override
    public List<NotificationDeliveryRecord> findByNotificationId(String notificationId) {
        return mapper.selectList(new QueryWrapper<NotificationDeliveryRecordEntity>()
                        .eq("notification_id", notificationId)
                        .orderByAsc("channel"))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private NotificationDeliveryRecord toDomain(NotificationDeliveryRecordEntity entity) {
        return new NotificationDeliveryRecord(
                entity.getDeliveryId(),
                entity.getNotificationId(),
                NotificationDeliveryChannel.valueOf(entity.getChannel()),
                NotificationDeliveryStatus.valueOf(entity.getStatus()),
                entity.getAttemptCount() == null ? 0 : entity.getAttemptCount(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDeliveredAt(),
                entity.getLastErrorCode()
        );
    }

    private NotificationDeliveryRecordEntity toEntity(
            NotificationDeliveryRecord record,
            NotificationDeliveryRecordEntity existing
    ) {
        NotificationDeliveryRecordEntity entity = existing == null ? new NotificationDeliveryRecordEntity() : existing;
        entity.setDeliveryId(record.deliveryId());
        entity.setNotificationId(record.notificationId());
        entity.setChannel(record.channel().name());
        entity.setStatus(record.status().name());
        entity.setAttemptCount(record.attemptCount());
        entity.setCreatedAt(record.createdAt());
        entity.setUpdatedAt(record.updatedAt());
        entity.setDeliveredAt(record.deliveredAt());
        entity.setLastErrorCode(record.lastErrorCode());
        return entity;
    }
}
