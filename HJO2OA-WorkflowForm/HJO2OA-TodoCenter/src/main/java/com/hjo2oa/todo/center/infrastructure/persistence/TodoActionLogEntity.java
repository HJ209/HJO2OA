package com.hjo2oa.todo.center.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("wf_todo_action_log")
public class TodoActionLogEntity {

    @TableId(value = "idempotency_key", type = IdType.INPUT)
    private String idempotencyKey;

    @TableField("action_type")
    private String actionType;

    @TableField("target_id")
    private String targetId;

    @TableField("processed_at")
    private Instant processedAt;
}
