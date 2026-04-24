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
@TableName("infra_cache_invalidation")
public class CacheInvalidationRecordEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("cache_policy_id")
    private UUID cachePolicyId;

    @TableField("invalidate_key")
    private String invalidateKey;

    @TableField("reason_type")
    private String reasonType;

    @TableField("reason_ref")
    private String reasonRef;

    @TableField("invalidated_at")
    private Instant invalidatedAt;
}
