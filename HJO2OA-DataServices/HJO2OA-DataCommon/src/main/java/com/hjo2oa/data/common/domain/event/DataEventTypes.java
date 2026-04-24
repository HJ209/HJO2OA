package com.hjo2oa.data.common.domain.event;

public final class DataEventTypes {

    public static final String DATA_SERVICE_ACTIVATED = "data.service.activated";
    public static final String DATA_API_PUBLISHED = "data.api.published";
    public static final String DATA_API_DEPRECATED = "data.api.deprecated";
    public static final String DATA_CONNECTOR_UPDATED = "data.connector.updated";
    public static final String DATA_SYNC_COMPLETED = "data.sync.completed";
    public static final String DATA_SYNC_FAILED = "data.sync.failed";
    public static final String DATA_REPORT_REFRESHED = "data.report.refreshed";
    public static final String DATA_GOVERNANCE_ALERTED = "data.governance.alerted";

    private DataEventTypes() {
    }
}
