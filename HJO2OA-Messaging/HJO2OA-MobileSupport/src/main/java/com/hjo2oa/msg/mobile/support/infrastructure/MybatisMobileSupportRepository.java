package com.hjo2oa.msg.mobile.support.infrastructure;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hjo2oa.msg.mobile.support.domain.DeviceBindStatus;
import com.hjo2oa.msg.mobile.support.domain.DeviceBinding;
import com.hjo2oa.msg.mobile.support.domain.MobileAppType;
import com.hjo2oa.msg.mobile.support.domain.MobilePlatform;
import com.hjo2oa.msg.mobile.support.domain.MobileRiskLevel;
import com.hjo2oa.msg.mobile.support.domain.MobileSession;
import com.hjo2oa.msg.mobile.support.domain.MobileSessionStatus;
import com.hjo2oa.msg.mobile.support.domain.MobileSupportRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnBean(DataSource.class)
public class MybatisMobileSupportRepository implements MobileSupportRepository {

    private static final Comparator<MobileSession> SESSION_ORDER =
            Comparator.comparing(MobileSession::updatedAt).reversed();

    private final DeviceBindingMapper deviceBindingMapper;
    private final MobileSessionMapper mobileSessionMapper;

    public MybatisMobileSupportRepository(
            DeviceBindingMapper deviceBindingMapper,
            MobileSessionMapper mobileSessionMapper
    ) {
        this.deviceBindingMapper = Objects.requireNonNull(deviceBindingMapper, "deviceBindingMapper must not be null");
        this.mobileSessionMapper = Objects.requireNonNull(mobileSessionMapper, "mobileSessionMapper must not be null");
    }

    @Override
    public Optional<DeviceBinding> findDeviceBindingById(UUID id) {
        return Optional.ofNullable(deviceBindingMapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<DeviceBinding> findDeviceBindingByTenantAndDeviceId(UUID tenantId, String deviceId) {
        return deviceBindingMapper.selectList(Wrappers.<DeviceBindingEntity>lambdaQuery()
                        .eq(DeviceBindingEntity::getTenantId, tenantId)
                        .eq(DeviceBindingEntity::getDeviceId, deviceId)
                        .orderByDesc(DeviceBindingEntity::getUpdatedAt))
                .stream()
                .findFirst()
                .map(this::toDomain);
    }

    @Override
    public List<DeviceBinding> findDeviceBindingsByPerson(UUID tenantId, UUID personId, DeviceBindStatus bindStatus) {
        return deviceBindingMapper.selectList(Wrappers.<DeviceBindingEntity>lambdaQuery()
                        .eq(DeviceBindingEntity::getTenantId, tenantId)
                        .eq(DeviceBindingEntity::getPersonId, personId)
                        .eq(bindStatus != null, DeviceBindingEntity::getBindStatus,
                                bindStatus == null ? null : bindStatus.name())
                        .orderByDesc(DeviceBindingEntity::getUpdatedAt))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public DeviceBinding saveDeviceBinding(DeviceBinding deviceBinding) {
        DeviceBindingEntity existing = deviceBindingMapper.selectById(deviceBinding.id());
        DeviceBindingEntity entity = toEntity(deviceBinding, existing);
        if (existing == null) {
            deviceBindingMapper.insert(entity);
        } else {
            deviceBindingMapper.updateById(entity);
        }
        return findDeviceBindingById(deviceBinding.id()).orElseThrow();
    }

    @Override
    public Optional<MobileSession> findMobileSessionById(UUID sessionId) {
        return Optional.ofNullable(mobileSessionMapper.selectById(sessionId)).map(this::toDomain);
    }

    @Override
    public List<MobileSession> findMobileSessionsByDeviceBinding(
            UUID tenantId,
            UUID deviceBindingId,
            MobileSessionStatus status
    ) {
        return mobileSessionMapper.selectList(Wrappers.<MobileSessionEntity>lambdaQuery()
                        .eq(MobileSessionEntity::getTenantId, tenantId)
                        .eq(MobileSessionEntity::getDeviceBindingId, deviceBindingId)
                        .eq(status != null, MobileSessionEntity::getSessionStatus, status == null ? null : status.name())
                        .orderByDesc(MobileSessionEntity::getUpdatedAt))
                .stream()
                .map(this::toDomain)
                .sorted(SESSION_ORDER)
                .toList();
    }

    @Override
    public List<MobileSession> findMobileSessionsByPerson(UUID tenantId, UUID personId, MobileSessionStatus status) {
        return mobileSessionMapper.selectList(Wrappers.<MobileSessionEntity>lambdaQuery()
                        .eq(MobileSessionEntity::getTenantId, tenantId)
                        .eq(MobileSessionEntity::getPersonId, personId)
                        .eq(status != null, MobileSessionEntity::getSessionStatus, status == null ? null : status.name())
                        .orderByDesc(MobileSessionEntity::getUpdatedAt))
                .stream()
                .map(this::toDomain)
                .sorted(SESSION_ORDER)
                .toList();
    }

    @Override
    public MobileSession saveMobileSession(MobileSession mobileSession) {
        MobileSessionEntity existing = mobileSessionMapper.selectById(mobileSession.id());
        MobileSessionEntity entity = toEntity(mobileSession, existing);
        if (existing == null) {
            mobileSessionMapper.insert(entity);
        } else {
            mobileSessionMapper.updateById(entity);
        }
        return findMobileSessionById(mobileSession.id()).orElseThrow();
    }

    private DeviceBinding toDomain(DeviceBindingEntity entity) {
        return new DeviceBinding(
                entity.getId(),
                entity.getPersonId(),
                entity.getAccountId(),
                entity.getDeviceId(),
                entity.getDeviceFingerprint(),
                MobilePlatform.valueOf(entity.getPlatform()),
                MobileAppType.valueOf(entity.getAppType()),
                entity.getPushToken(),
                DeviceBindStatus.valueOf(entity.getBindStatus()),
                MobileRiskLevel.valueOf(entity.getRiskLevel()),
                entity.getLastLoginAt(),
                entity.getLastSeenAt(),
                entity.getTenantId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private MobileSession toDomain(MobileSessionEntity entity) {
        return new MobileSession(
                entity.getId(),
                entity.getDeviceBindingId(),
                entity.getPersonId(),
                entity.getAccountId(),
                entity.getCurrentAssignmentId(),
                entity.getCurrentPositionId(),
                MobileSessionStatus.valueOf(entity.getSessionStatus()),
                MobileRiskLevel.valueOf(entity.getRiskLevelSnapshot()),
                entity.getRiskFrozenAt(),
                entity.getRiskReason(),
                entity.getIssuedAt(),
                entity.getExpiresAt(),
                entity.getLastHeartbeatAt(),
                entity.getRefreshVersion() == null ? 0 : entity.getRefreshVersion(),
                entity.getTenantId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private DeviceBindingEntity toEntity(DeviceBinding binding, DeviceBindingEntity existing) {
        DeviceBindingEntity entity = existing == null ? new DeviceBindingEntity() : existing;
        entity.setId(binding.id());
        entity.setPersonId(binding.personId());
        entity.setAccountId(binding.accountId());
        entity.setDeviceId(binding.deviceId());
        entity.setDeviceFingerprint(binding.deviceFingerprint());
        entity.setPlatform(binding.platform().name());
        entity.setAppType(binding.appType().name());
        entity.setPushToken(binding.pushToken());
        entity.setBindStatus(binding.bindStatus().name());
        entity.setRiskLevel(binding.riskLevel().name());
        entity.setLastLoginAt(binding.lastLoginAt());
        entity.setLastSeenAt(binding.lastSeenAt());
        entity.setTenantId(binding.tenantId());
        entity.setCreatedAt(binding.createdAt());
        entity.setUpdatedAt(binding.updatedAt());
        return entity;
    }

    private MobileSessionEntity toEntity(MobileSession session, MobileSessionEntity existing) {
        MobileSessionEntity entity = existing == null ? new MobileSessionEntity() : existing;
        entity.setId(session.id());
        entity.setDeviceBindingId(session.deviceBindingId());
        entity.setPersonId(session.personId());
        entity.setAccountId(session.accountId());
        entity.setCurrentAssignmentId(session.currentAssignmentId());
        entity.setCurrentPositionId(session.currentPositionId());
        entity.setSessionStatus(session.sessionStatus().name());
        entity.setRiskLevelSnapshot(session.riskLevelSnapshot().name());
        entity.setRiskFrozenAt(session.riskFrozenAt());
        entity.setRiskReason(session.riskReason());
        entity.setIssuedAt(session.issuedAt());
        entity.setExpiresAt(session.expiresAt());
        entity.setLastHeartbeatAt(session.lastHeartbeatAt());
        entity.setRefreshVersion(session.refreshVersion());
        entity.setTenantId(session.tenantId());
        entity.setCreatedAt(session.createdAt());
        entity.setUpdatedAt(session.updatedAt());
        return entity;
    }
}
