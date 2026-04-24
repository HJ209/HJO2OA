package com.hjo2oa.data.common.domain.exception;

import com.hjo2oa.shared.kernel.ErrorDescriptor;
import org.springframework.http.HttpStatus;

public enum DataServicesErrorCode {

    DATA_COMMON_BAD_REQUEST("DATA-COMMON-400", HttpStatus.BAD_REQUEST, "数据服务请求不合法"),
    DATA_COMMON_UNAUTHORIZED("DATA-COMMON-401", HttpStatus.UNAUTHORIZED, "数据服务调用未认证"),
    DATA_COMMON_FORBIDDEN("DATA-COMMON-403", HttpStatus.FORBIDDEN, "数据服务调用无权限"),
    DATA_COMMON_NOT_FOUND("DATA-COMMON-404", HttpStatus.NOT_FOUND, "数据服务资源不存在"),
    DATA_COMMON_CONFLICT("DATA-COMMON-409", HttpStatus.CONFLICT, "数据服务资源冲突"),
    DATA_COMMON_VALIDATION_ERROR("DATA-COMMON-422", HttpStatus.UNPROCESSABLE_ENTITY, "数据服务参数校验失败"),
    DATA_COMMON_INTERNAL_ERROR("DATA-COMMON-500", HttpStatus.INTERNAL_SERVER_ERROR, "数据服务内部错误"),
    DATA_SERVICE_NOT_FOUND("DATA-SERVICE-404", HttpStatus.NOT_FOUND, "数据服务定义不存在"),
    OPEN_API_ENDPOINT_NOT_FOUND("DATA-OPENAPI-404", HttpStatus.NOT_FOUND, "开放接口不存在"),
    CONNECTOR_NOT_FOUND("DATA-CONNECTOR-404", HttpStatus.NOT_FOUND, "连接器不存在"),
    CONNECTOR_NOT_ACTIVE("DATA-CONNECTOR-409", HttpStatus.CONFLICT, "连接器未启用"),
    SYNC_TASK_NOT_FOUND("DATA-SYNC-404", HttpStatus.NOT_FOUND, "同步任务不存在"),
    REPORT_DEFINITION_NOT_FOUND("DATA-REPORT-404", HttpStatus.NOT_FOUND, "报表定义不存在"),
    GOVERNANCE_PROFILE_NOT_FOUND("DATA-GOVERNANCE-404", HttpStatus.NOT_FOUND, "治理配置不存在");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    DataServicesErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public String code() {
        return code;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public String defaultMessage() {
        return defaultMessage;
    }

    public ErrorDescriptor descriptor() {
        return new ErrorDescriptor(code, httpStatus, defaultMessage);
    }
}
