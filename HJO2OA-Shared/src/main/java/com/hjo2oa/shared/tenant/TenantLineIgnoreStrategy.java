package com.hjo2oa.shared.tenant;

import java.util.Locale;
import java.util.Set;

public final class TenantLineIgnoreStrategy {

    private static final Set<String> TENANT_SCOPED_TABLES = Set.of(
            "data_connector_def",
            "data_service_def",
            "data_service_version",
            "data_service_subscription",
            "data_sync_task",
            "data_sync_mapping_rule",
            "data_sync_execution_record",
            "data_open_api_endpoint",
            "data_api_rate_limit_policy",
            "data_api_credential_grant",
            "data_api_invocation_audit_log",
            "data_api_quota_usage_counter",
            "data_report_def",
            "data_report_snapshot",
            "data_governance_profile",
            "data_governance_runtime_signal",
            "infra_dictionary_type",
            "infra_locale_bundle",
            "infra_timezone_setting",
            "infra_translation_entry",
            "infra_scheduled_job",
            "infra_attachment_asset",
            "infra_event_message",
            "infra_audit_record",
            "infra_security_policy",
            "org_organization",
            "org_department",
            "org_person",
            "org_account",
            "org_position",
            "org_assignment",
            "org_position_role",
            "org_role",
            "org_resource_permission",
            "org_person_role",
            "org_data_permission",
            "org_field_permission",
            "org_sync_source_config",
            "org_sync_task",
            "org_sync_diff_record",
            "org_sync_conflict_record",
            "org_sync_compensation_record",
            "org_audit_record",
            "proc_definition",
            "proc_action_def",
            "form_metadata",
            "proc_instance",
            "proc_task",
            "msg_subscription_rule",
            "msg_subscription_preference",
            "msg_template",
            "msg_channel_endpoint",
            "msg_routing_policy",
            "msg_delivery_task",
            "msg_device_binding",
            "msg_mobile_session",
            "msg_ecosystem_integration",
            "portal_widget_definition",
            "portal_card_snapshot",
            "portal_home_refresh_state",
            "portal_template",
            "portal_publication",
            "portal_widget_reference",
            "portal_personalization_profile",
            "portal_designer_template_projection",
            "msg_notification",
            "wf_action_engine_definition",
            "wf_action_engine_execution"
    );

    private static final Set<String> EXPLICITLY_IGNORED_TABLES = Set.of(
            "infra_tenant_profile",
            "infra_tenant_quota",
            "infra_config_entry",
            "infra_config_override",
            "infra_feature_rule",
            "flyway_schema_history"
    );

    private TenantLineIgnoreStrategy() {
    }

    public static boolean ignoreTable(String tableName) {
        String normalized = normalize(tableName);
        if (normalized.isBlank()) {
            return true;
        }
        return EXPLICITLY_IGNORED_TABLES.contains(normalized) || !TENANT_SCOPED_TABLES.contains(normalized);
    }

    public static boolean isTenantScoped(String tableName) {
        String normalized = normalize(tableName);
        return TENANT_SCOPED_TABLES.contains(normalized) && !EXPLICITLY_IGNORED_TABLES.contains(normalized);
    }

    public static Set<String> tenantScopedTables() {
        return TENANT_SCOPED_TABLES;
    }

    public static Set<String> explicitlyIgnoredTables() {
        return EXPLICITLY_IGNORED_TABLES;
    }

    private static String normalize(String tableName) {
        if (tableName == null) {
            return "";
        }
        String normalized = tableName.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("dbo.")) {
            normalized = normalized.substring("dbo.".length());
        }
        return normalized.replace("[", "").replace("]", "").replace("\"", "");
    }
}
