package com.hjo2oa.infra.cache.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("infra_cache_policy")
public class CachePolicyEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("namespace")
    private String namespace;

    @TableField("backend_type")
    private String backendType;

    @TableField("ttl_seconds")
    private Integer ttlSeconds;

    @TableField("max_capacity")
    private Integer maxCapacity;

    @TableField("eviction_policy")
    private String evictionPolicy;

    @TableField("invalidation_mode")
    private String invalidationMode;

    @TableField("metrics_enabled")
    private Boolean metricsEnabled;

    @TableField("active")
    private Boolean active;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
