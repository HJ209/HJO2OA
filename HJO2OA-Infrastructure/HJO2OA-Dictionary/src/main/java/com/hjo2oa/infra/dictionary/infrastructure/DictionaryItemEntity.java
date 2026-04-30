package com.hjo2oa.infra.dictionary.infrastructure;

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
@TableName("infra_dictionary_item")
public class DictionaryItemEntity {

    @TableId(value = "item_id", type = IdType.INPUT)
    private UUID id;

    @TableField("dictionary_type_id")
    private UUID dictionaryTypeId;

    @TableField("item_code")
    private String itemCode;

    @TableField("display_name")
    private String displayName;

    @TableField("parent_item_id")
    private UUID parentItemId;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("multi_lang_value")
    private String multiLangValue;

    @TableField("default_item")
    private Boolean defaultItem;

    @TableField("extension_json")
    private String extensionJson;
}
