package com.hjo2oa.infra.data.i18n.infrastructure;

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
@TableName("infra_translation_entry")
public class TranslationEntryEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("entity_type")
    private String entityType;

    @TableField("entity_id")
    private String entityId;

    @TableField("field_name")
    private String fieldName;

    @TableField("locale")
    private String locale;

    @TableField("translated_value")
    private String translatedValue;

    @TableField("translation_status")
    private String translationStatus;

    @TableField("tenant_id")
    private UUID tenantId;

    @TableField("updated_by")
    private UUID updatedBy;

    @TableField("updated_at")
    private Instant updatedAt;
}
