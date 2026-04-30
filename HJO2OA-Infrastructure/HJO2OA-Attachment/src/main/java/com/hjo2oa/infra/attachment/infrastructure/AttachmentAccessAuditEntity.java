package com.hjo2oa.infra.attachment.infrastructure;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("infra_attachment_access_audit")
public class AttachmentAccessAuditEntity {

    @TableId
    private String id;
    private String attachmentAssetId;
    private Integer versionNo;
    private String action;
    private String tenantId;
    private String operatorId;
    private String clientIp;
    private LocalDateTime occurredAt;
}
