package com.hjo2oa.wf.form.renderer.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("wf_form_submission")
public class FormSubmissionEntity {

    @TableId(value = "submission_id", type = IdType.INPUT)
    private UUID submissionId;

    @TableField("metadata_id")
    private UUID metadataId;

    @TableField("metadata_code")
    private String metadataCode;

    @TableField("metadata_version")
    private Integer metadataVersion;

    @TableField("process_instance_id")
    private UUID processInstanceId;

    @TableField("form_data_id")
    private UUID formDataId;

    @TableField("node_id")
    private String nodeId;

    @TableField("status")
    private String status;

    @TableField("form_data")
    private String formData;

    @TableField("attachment_ids")
    private String attachmentIds;

    @TableField("validation_result")
    private String validationResult;

    @TableField("idempotency_key")
    private String idempotencyKey;

    @TableField("submitted_by")
    private UUID submittedBy;

    @TableField("tenant_id")
    private UUID tenantId;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;

    @TableField("submitted_at")
    private Instant submittedAt;
}
