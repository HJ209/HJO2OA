package com.hjo2oa.org.data.permission.infrastructure;

import com.hjo2oa.org.data.permission.domain.SubjectReference;
import java.util.List;
import java.util.UUID;

public final class DataPermissionRuntimeContext {

    private static final ThreadLocal<DataPermissionRuntimeContext> CURRENT = new ThreadLocal<>();

    private final String businessObject;
    private final UUID tenantId;
    private final List<SubjectReference> subjects;

    private DataPermissionRuntimeContext(String businessObject, UUID tenantId, List<SubjectReference> subjects) {
        this.businessObject = businessObject;
        this.tenantId = tenantId;
        this.subjects = List.copyOf(subjects);
    }

    public static void set(String businessObject, UUID tenantId, List<SubjectReference> subjects) {
        CURRENT.set(new DataPermissionRuntimeContext(businessObject, tenantId, subjects));
    }

    public static DataPermissionRuntimeContext current() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    public String businessObject() {
        return businessObject;
    }

    public UUID tenantId() {
        return tenantId;
    }

    public List<SubjectReference> subjects() {
        return subjects;
    }
}
