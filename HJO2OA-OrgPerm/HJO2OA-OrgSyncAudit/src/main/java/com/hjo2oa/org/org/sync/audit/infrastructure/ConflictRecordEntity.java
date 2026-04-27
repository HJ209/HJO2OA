package com.hjo2oa.org.org.sync.audit.infrastructure;

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
@TableName("org_sync_conflict_record")
public class ConflictRecordEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("tenant_id")
    private UUID tenantId;

    @TableField("diff_record_id")
    private UUID diffRecordId;

    @TableField("conflict_field")
    private String conflictField;

    @TableField("source_value")
    private String sourceValue;

    @TableField("local_value")
    private String localValue;

    @TableField("severity")
    private String severity;

    @TableField("created_at")
    private Instant createdAt;
}
