package com.hjo2oa.infra.attachment.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("infra_attachment_binding")
public class AttachmentBindingEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("attachment_asset_id")
    private String attachmentAssetId;

    @TableField("business_type")
    private String businessType;

    @TableField("business_id")
    private String businessId;

    @TableField("binding_role")
    private String bindingRole;

    @TableField("active")
    private Boolean active;
}
