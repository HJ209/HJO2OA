package com.hjo2oa.infra.dictionary.infrastructure;

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
@TableName("infra_dictionary_type")
public class DictionaryTypeEntity {

    @TableId(value = "dictionary_type_id", type = IdType.INPUT)
    private UUID id;

    @TableField("code")
    private String code;

    @TableField("name")
    private String name;

    @TableField("category")
    private String category;

    @TableField("hierarchical")
    private Boolean hierarchical;

    @TableField("cacheable")
    private Boolean cacheable;

    @TableField("status")
    private String status;

    @TableField("tenant_id")
    private UUID tenantId;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
