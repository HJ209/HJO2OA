package com.hjo2oa.data.report.application;

import com.hjo2oa.shared.kernel.ErrorDescriptor;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import org.springframework.http.HttpStatus;

public final class ReportErrorDescriptors {

    public static final ErrorDescriptor REPORT_NOT_FOUND =
            SharedErrorDescriptors.of("REPORT_NOT_FOUND", HttpStatus.NOT_FOUND, "报表定义不存在");
    public static final ErrorDescriptor REPORT_DEFINITION_CONFLICT =
            SharedErrorDescriptors.of("REPORT_DEFINITION_CONFLICT", HttpStatus.CONFLICT, "报表定义编码冲突");
    public static final ErrorDescriptor REPORT_RULE_VIOLATION =
            SharedErrorDescriptors.of("REPORT_RULE_VIOLATION", HttpStatus.UNPROCESSABLE_ENTITY, "报表口径定义不合法");
    public static final ErrorDescriptor REPORT_DATA_SOURCE_MISSING =
            SharedErrorDescriptors.of("REPORT_DATA_SOURCE_MISSING", HttpStatus.UNPROCESSABLE_ENTITY, "报表统计源未注册");
    public static final ErrorDescriptor REPORT_SNAPSHOT_NOT_READY =
            SharedErrorDescriptors.of("REPORT_SNAPSHOT_NOT_READY", HttpStatus.CONFLICT, "报表快照尚未就绪");
    public static final ErrorDescriptor REPORT_CARD_NOT_AVAILABLE =
            SharedErrorDescriptors.of("REPORT_CARD_NOT_AVAILABLE", HttpStatus.UNPROCESSABLE_ENTITY, "当前报表未开放图卡数据源");

    private ReportErrorDescriptors() {
    }
}
