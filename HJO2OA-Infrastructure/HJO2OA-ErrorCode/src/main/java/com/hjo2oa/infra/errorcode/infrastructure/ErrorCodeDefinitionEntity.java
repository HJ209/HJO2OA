package com.hjo2oa.infra.errorcode.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("infra_error_code_def")
public class ErrorCodeDefinitionEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("code")
    private String code;

    @TableField("module_code")
    private String moduleCode;

    @TableField("category")
    private String category;

    @TableField("severity")
    private String severity;

    @TableField("http_status")
    private Integer httpStatus;

    @TableField("message_key")
    private String messageKey;

    @TableField("retryable")
    private Boolean retryable;

    @TableField("deprecated")
    private Boolean deprecated;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
