package com.hjo2oa.data.data.sync.domain;

import com.hjo2oa.shared.kernel.ErrorDescriptor;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import org.springframework.http.HttpStatus;

public final class DataSyncErrorDescriptors {

    public static final ErrorDescriptor TASK_NOT_FOUND =
            SharedErrorDescriptors.of("DATA_SYNC_TASK_NOT_FOUND", HttpStatus.NOT_FOUND, "同步任务不存在");
    public static final ErrorDescriptor TASK_CODE_DUPLICATE =
            SharedErrorDescriptors.of("DATA_SYNC_TASK_CODE_DUPLICATE", HttpStatus.CONFLICT, "同步任务编码重复");
    public static final ErrorDescriptor TASK_STATE_CONFLICT =
            SharedErrorDescriptors.of("DATA_SYNC_TASK_STATE_CONFLICT", HttpStatus.CONFLICT, "同步任务状态冲突");
    public static final ErrorDescriptor TASK_MAPPING_INVALID =
            SharedErrorDescriptors.of(
                    "DATA_SYNC_TASK_MAPPING_INVALID",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "同步映射规则不合法"
            );
    public static final ErrorDescriptor CHECKPOINT_RESET_FORBIDDEN =
            SharedErrorDescriptors.of(
                    "DATA_SYNC_CHECKPOINT_RESET_FORBIDDEN",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "当前任务不允许重置检查点"
            );
    public static final ErrorDescriptor CONNECTOR_UNAVAILABLE =
            SharedErrorDescriptors.of(
                    "DATA_SYNC_CONNECTOR_UNAVAILABLE",
                    HttpStatus.CONFLICT,
                    "同步任务依赖的连接器不可用"
            );
    public static final ErrorDescriptor EXECUTION_NOT_FOUND =
            SharedErrorDescriptors.of("DATA_SYNC_EXECUTION_NOT_FOUND", HttpStatus.NOT_FOUND, "同步执行记录不存在");
    public static final ErrorDescriptor EXECUTION_NOT_RETRYABLE =
            SharedErrorDescriptors.of(
                    "DATA_SYNC_EXECUTION_NOT_RETRYABLE",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "当前执行记录不允许重试"
            );
    public static final ErrorDescriptor COMPENSATION_NOT_ALLOWED =
            SharedErrorDescriptors.of(
                    "DATA_SYNC_COMPENSATION_NOT_ALLOWED",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "当前任务不允许人工补偿"
            );
    public static final ErrorDescriptor DIFFERENCE_ALREADY_RESOLVED =
            SharedErrorDescriptors.of(
                    "DATA_SYNC_DIFFERENCE_ALREADY_RESOLVED",
                    HttpStatus.CONFLICT,
                    "同步差异已处理，不能重复提交结论"
            );
    public static final ErrorDescriptor RECONCILIATION_NOT_ALLOWED =
            SharedErrorDescriptors.of(
                    "DATA_SYNC_RECONCILIATION_NOT_ALLOWED",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "当前任务未启用对账"
            );

    private DataSyncErrorDescriptors() {
    }
}
