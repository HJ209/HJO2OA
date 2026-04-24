package com.hjo2oa.data.common.audit;

import java.util.List;

public interface DataAuditRecorder {

    void record(DataAuditRecord record);

    List<DataAuditRecord> records();
}
