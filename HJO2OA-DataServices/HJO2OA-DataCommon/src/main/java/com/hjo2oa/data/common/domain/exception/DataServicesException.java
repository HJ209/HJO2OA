package com.hjo2oa.data.common.domain.exception;

import com.hjo2oa.shared.kernel.BizException;

public class DataServicesException extends BizException {

    private final DataServicesErrorCode errorCode;

    public DataServicesException(DataServicesErrorCode errorCode) {
        super(errorCode.descriptor());
        this.errorCode = errorCode;
    }

    public DataServicesException(DataServicesErrorCode errorCode, String message) {
        super(errorCode.descriptor(), message);
        this.errorCode = errorCode;
    }

    public DataServicesException(DataServicesErrorCode errorCode, String message, Throwable cause) {
        super(errorCode.descriptor(), message, cause);
        this.errorCode = errorCode;
    }

    public DataServicesErrorCode dataErrorCode() {
        return errorCode;
    }
}
