package com.hjo2oa.infra.i18n.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@TableName("infra_locale_resource_entry")
public class LocaleResourceEntryEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("locale_bundle_id")
    private UUID localeBundleId;

    @TableField("resource_key")
    private String resourceKey;

    @TableField("resource_value")
    private String resourceValue;

    @TableField("version")
    private Integer version;

    @TableField("active")
    private Boolean active;
}
