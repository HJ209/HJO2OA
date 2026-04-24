package com.hjo2oa.data.common.audit;

import com.hjo2oa.data.common.support.Require;

public record DataAuditFieldChange(
        String fieldName,
        String oldValue,
        String newValue,
        boolean sensitive
) {

    public DataAuditFieldChange {
        fieldName = Require.text(fieldName, "fieldName");
    }
}
