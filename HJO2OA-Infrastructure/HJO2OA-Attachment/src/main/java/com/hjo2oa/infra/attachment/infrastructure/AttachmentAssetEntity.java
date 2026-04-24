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
@TableName("infra_attachment_asset")
public class AttachmentAssetEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("storage_key")
    private String storageKey;

    @TableField("original_filename")
    private String originalFilename;

    @TableField("content_type")
    private String contentType;

    @TableField("size_bytes")
    private Long sizeBytes;

    @TableField("checksum")
    private String checksum;

    @TableField("storage_provider")
    private String storageProvider;

    @TableField("preview_status")
    private String previewStatus;

    @TableField("latest_version_no")
    private Integer latestVersionNo;

    @TableField("permission_mode")
    private String permissionMode;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("created_by")
    private String createdBy;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
