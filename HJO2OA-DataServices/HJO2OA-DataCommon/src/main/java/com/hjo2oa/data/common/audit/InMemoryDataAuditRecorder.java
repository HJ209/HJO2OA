package com.hjo2oa.data.common.audit;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

@Component
public class InMemoryDataAuditRecorder implements DataAuditRecorder {

    private final CopyOnWriteArrayList<DataAuditRecord> records = new CopyOnWriteArrayList<>();

    @Override
    public void record(DataAuditRecord record) {
        records.add(record);
    }

    @Override
    public List<DataAuditRecord> records() {
        return List.copyOf(records);
    }
}
