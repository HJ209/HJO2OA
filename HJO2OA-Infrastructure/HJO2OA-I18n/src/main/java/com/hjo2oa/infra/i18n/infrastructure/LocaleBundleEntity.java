package com.hjo2oa.infra.i18n.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@TableName("infra_locale_bundle")
public class LocaleBundleEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("bundle_code")
    private String bundleCode;

    @TableField("module_code")
    private String moduleCode;

    @TableField("locale")
    private String locale;

    @TableField("fallback_locale")
    private String fallbackLocale;

    @TableField("status")
    private String status;

    @TableField("tenant_id")
    private UUID tenantId;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
