package com.hjo2oa.shared.web;

import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;

public class IdempotencyConflictException extends BizException {

    public IdempotencyConflictException(String message) {
        super(SharedErrorDescriptors.IDEMPOTENCY_CONFLICT, message);
    }
}
