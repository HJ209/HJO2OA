package com.hjo2oa.wf.form.metadata.infrastructure;

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
@TableName("form_metadata")
public class FormMetadataEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("code")
    private String code;

    @TableField("name")
    private String name;

    @TableField("name_i18n_key")
    private String nameI18nKey;

    @TableField("version")
    private Integer version;

    @TableField("status")
    private String status;

    @TableField("field_schema")
    private String fieldSchema;

    @TableField("layout")
    private String layout;

    @TableField("validations")
    private String validations;

    @TableField("field_permission_map")
    private String fieldPermissionMap;

    @TableField("tenant_id")
    private UUID tenantId;

    @TableField("published_at")
    private Instant publishedAt;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
