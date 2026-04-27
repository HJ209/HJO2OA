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
@TableName("org_account")
public class AccountEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("person_id")
    private UUID personId;

    @TableField("username")
    private String username;

    @TableField("credential")
    private String credential;

    @TableField("account_type")
    private String accountType;

    @TableField("is_primary")
    private Boolean primaryAccount;

    @TableField("locked")
    private Boolean locked;

    @TableField("locked_until")
    private Instant lockedUntil;

    @TableField("last_login_at")
    private Instant lastLoginAt;

    @TableField("last_login_ip")
    private String lastLoginIp;

    @TableField("password_changed_at")
    private Instant passwordChangedAt;

    @TableField("must_change_password")
    private Boolean mustChangePassword;

    @TableField("status")
    private String status;

    @TableField("tenant_id")
    private UUID tenantId;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
