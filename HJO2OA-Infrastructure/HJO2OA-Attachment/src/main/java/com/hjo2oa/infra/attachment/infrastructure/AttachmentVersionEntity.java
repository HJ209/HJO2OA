package com.hjo2oa.infra.attachment.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("infra_attachment_version")
public class AttachmentVersionEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("attachment_asset_id")
    private String attachmentAssetId;

    @TableField("version_no")
    private Integer versionNo;

    @TableField("storage_key")
    private String storageKey;

    @TableField("checksum")
    private String checksum;

    @TableField("size_bytes")
    private Long sizeBytes;

    @TableField("created_by")
    private String createdBy;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
