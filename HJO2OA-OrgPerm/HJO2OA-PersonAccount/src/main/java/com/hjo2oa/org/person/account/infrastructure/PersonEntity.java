package com.hjo2oa.org.person.account.infrastructure;

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
@TableName("org_person")
public class PersonEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("employee_no")
    private String employeeNo;

    @TableField("name")
    private String name;

    @TableField("pinyin")
    private String pinyin;

    @TableField("gender")
    private String gender;

    @TableField("mobile")
    private String mobile;

    @TableField("email")
    private String email;

    @TableField("organization_id")
    private UUID organizationId;

    @TableField("department_id")
    private UUID departmentId;

    @TableField("status")
    private String status;

    @TableField("tenant_id")
    private UUID tenantId;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
