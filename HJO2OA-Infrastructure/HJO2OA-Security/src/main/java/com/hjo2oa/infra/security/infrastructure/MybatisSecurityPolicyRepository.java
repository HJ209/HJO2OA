package com.hjo2oa.infra.security.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hjo2oa.infra.security.domain.KeyStatus;
import com.hjo2oa.infra.security.domain.MaskingRule;
import com.hjo2oa.infra.security.domain.RateLimitRule;
import com.hjo2oa.infra.security.domain.RateLimitSubjectType;
import com.hjo2oa.infra.security.domain.SecretKeyMaterial;
import com.hjo2oa.infra.security.domain.SecurityPolicy;
import com.hjo2oa.infra.security.domain.SecurityPolicyRepository;
import com.hjo2oa.infra.security.domain.SecurityPolicyStatus;
import com.hjo2oa.infra.security.domain.SecurityPolicyType;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Primary
@Repository
public class MybatisSecurityPolicyRepository implements SecurityPolicyRepository {

    private final SecurityPolicyMapper securityPolicyMapper;
    private final SecretKeyMaterialMapper secretKeyMaterialMapper;
    private final MaskingRuleMapper maskingRuleMapper;
    private final RateLimitRuleMapper rateLimitRuleMapper;

    public MybatisSecurityPolicyRepository(
            SecurityPolicyMapper securityPolicyMapper,
            SecretKeyMaterialMapper secretKeyMaterialMapper,
            MaskingRuleMapper maskingRuleMapper,
            RateLimitRuleMapper rateLimitRuleMapper
    ) {
        this.securityPolicyMapper = Objects.requireNonNull(securityPolicyMapper, "securityPolicyMapper must not be null");
        this.secretKeyMaterialMapper = Objects.requireNonNull(
                secretKeyMaterialMapper,
                "secretKeyMaterialMapper must not be null"
        );
        this.maskingRuleMapper = Objects.requireNonNull(maskingRuleMapper, "maskingRuleMapper must not be null");
        this.rateLimitRuleMapper = Objects.requireNonNull(rateLimitRuleMapper, "rateLimitRuleMapper must not be null");
    }

    @Override
    public Optional<SecurityPolicy> findById(UUID id) {
        SecurityPolicyEntity entity = securityPolicyMapper.selectOne(new QueryWrapper<SecurityPolicyEntity>()
                .eq("id", toValue(id)));
        return Optional.ofNullable(entity).map(this::toDomain);
    }

    @Override
    public Optional<SecurityPolicy> findByPolicyCode(String policyCode) {
        SecurityPolicyEntity entity = securityPolicyMapper.selectOne(new QueryWrapper<SecurityPolicyEntity>()
                .eq("policy_code", policyCode));
        return Optional.ofNullable(entity).map(this::toDomain);
    }

    @Override
    public List<SecurityPolicy> findAll() {
        return securityPolicyMapper.selectList(new QueryWrapper<SecurityPolicyEntity>().orderByDesc("updated_at"))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public SecurityPolicy save(SecurityPolicy securityPolicy) {
        SecurityPolicyEntity entity = toEntity(securityPolicy);
        if (securityPolicyMapper.selectById(entity.getId()) == null) {
            securityPolicyMapper.insert(entity);
        } else {
            securityPolicyMapper.updateById(entity);
        }

        QueryWrapper<SecretKeyMaterialEntity> secretKeyDelete = new QueryWrapper<SecretKeyMaterialEntity>()
                .eq("security_policy_id", entity.getId());
        secretKeyMaterialMapper.delete(secretKeyDelete);
        for (SecretKeyMaterial secretKey : securityPolicy.secretKeys()) {
            secretKeyMaterialMapper.insert(toEntity(secretKey));
        }

        QueryWrapper<MaskingRuleEntity> maskingRuleDelete = new QueryWrapper<MaskingRuleEntity>()
                .eq("security_policy_id", entity.getId());
        maskingRuleMapper.delete(maskingRuleDelete);
        for (MaskingRule maskingRule : securityPolicy.maskingRules()) {
            maskingRuleMapper.insert(toEntity(maskingRule));
        }

        QueryWrapper<RateLimitRuleEntity> rateLimitRuleDelete = new QueryWrapper<RateLimitRuleEntity>()
                .eq("security_policy_id", entity.getId());
        rateLimitRuleMapper.delete(rateLimitRuleDelete);
        for (RateLimitRule rateLimitRule : securityPolicy.rateLimitRules()) {
            rateLimitRuleMapper.insert(toEntity(rateLimitRule));
        }

        return findById(securityPolicy.id()).orElseThrow();
    }

    private SecurityPolicy toDomain(SecurityPolicyEntity entity) {
        return new SecurityPolicy(
                toUuid(entity.getId()),
                entity.getPolicyCode(),
                SecurityPolicyType.valueOf(entity.getPolicyType()),
                entity.getName(),
                SecurityPolicyStatus.valueOf(entity.getStatus()),
                toUuid(entity.getTenantId()),
                entity.getConfigSnapshot(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                secretKeyMaterialMapper.selectList(new QueryWrapper<SecretKeyMaterialEntity>()
                                .eq("security_policy_id", entity.getId())
                                .orderByAsc("key_ref"))
                        .stream()
                        .map(this::toDomain)
                        .toList(),
                maskingRuleMapper.selectList(new QueryWrapper<MaskingRuleEntity>()
                                .eq("security_policy_id", entity.getId())
                                .orderByAsc("data_type"))
                        .stream()
                        .map(this::toDomain)
                        .toList(),
                rateLimitRuleMapper.selectList(new QueryWrapper<RateLimitRuleEntity>()
                                .eq("security_policy_id", entity.getId())
                                .orderByAsc("subject_type"))
                        .stream()
                        .map(this::toDomain)
                        .toList()
        );
    }

    private SecretKeyMaterial toDomain(SecretKeyMaterialEntity entity) {
        return new SecretKeyMaterial(
                toUuid(entity.getId()),
                toUuid(entity.getSecurityPolicyId()),
                entity.getKeyRef(),
                entity.getAlgorithm(),
                KeyStatus.valueOf(entity.getKeyStatus()),
                entity.getRotatedAt()
        );
    }

    private MaskingRule toDomain(MaskingRuleEntity entity) {
        return new MaskingRule(
                toUuid(entity.getId()),
                toUuid(entity.getSecurityPolicyId()),
                entity.getDataType(),
                entity.getRuleExpr(),
                Boolean.TRUE.equals(entity.getActive())
        );
    }

    private RateLimitRule toDomain(RateLimitRuleEntity entity) {
        return new RateLimitRule(
                toUuid(entity.getId()),
                toUuid(entity.getSecurityPolicyId()),
                RateLimitSubjectType.valueOf(entity.getSubjectType()),
                entity.getWindowSeconds(),
                entity.getMaxRequests(),
                Boolean.TRUE.equals(entity.getActive())
        );
    }

    private SecurityPolicyEntity toEntity(SecurityPolicy securityPolicy) {
        return new SecurityPolicyEntity()
                .setId(toValue(securityPolicy.id()))
                .setPolicyCode(securityPolicy.policyCode())
                .setPolicyType(securityPolicy.policyType().name())
                .setName(securityPolicy.name())
                .setStatus(securityPolicy.status().name())
                .setTenantId(toValue(securityPolicy.tenantId()))
                .setConfigSnapshot(securityPolicy.configSnapshot())
                .setCreatedAt(securityPolicy.createdAt())
                .setUpdatedAt(securityPolicy.updatedAt());
    }

    private SecretKeyMaterialEntity toEntity(SecretKeyMaterial secretKeyMaterial) {
        return new SecretKeyMaterialEntity()
                .setId(toValue(secretKeyMaterial.id()))
                .setSecurityPolicyId(toValue(secretKeyMaterial.securityPolicyId()))
                .setKeyRef(secretKeyMaterial.keyRef())
                .setAlgorithm(secretKeyMaterial.algorithm())
                .setKeyStatus(secretKeyMaterial.keyStatus().name())
                .setRotatedAt(secretKeyMaterial.rotatedAt());
    }

    private MaskingRuleEntity toEntity(MaskingRule maskingRule) {
        return new MaskingRuleEntity()
                .setId(toValue(maskingRule.id()))
                .setSecurityPolicyId(toValue(maskingRule.securityPolicyId()))
                .setDataType(maskingRule.dataType())
                .setRuleExpr(maskingRule.ruleExpr())
                .setActive(maskingRule.active());
    }

    private RateLimitRuleEntity toEntity(RateLimitRule rateLimitRule) {
        return new RateLimitRuleEntity()
                .setId(toValue(rateLimitRule.id()))
                .setSecurityPolicyId(toValue(rateLimitRule.securityPolicyId()))
                .setSubjectType(rateLimitRule.subjectType().name())
                .setWindowSeconds(rateLimitRule.windowSeconds())
                .setMaxRequests(rateLimitRule.maxRequests())
                .setActive(rateLimitRule.active());
    }

    private String toValue(UUID value) {
        return value == null ? null : value.toString();
    }

    private UUID toUuid(String value) {
        return value == null || value.isBlank() ? null : UUID.fromString(value);
    }
}
