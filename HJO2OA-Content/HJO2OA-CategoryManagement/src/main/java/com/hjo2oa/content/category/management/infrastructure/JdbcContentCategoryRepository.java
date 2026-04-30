package com.hjo2oa.content.category.management.infrastructure;

import com.hjo2oa.content.category.management.application.ContentCategoryApplicationService.CategoryRecord;
import com.hjo2oa.content.category.management.application.ContentCategoryApplicationService.CategoryStatus;
import com.hjo2oa.content.category.management.application.ContentCategoryApplicationService.PermissionEffect;
import com.hjo2oa.content.category.management.application.ContentCategoryApplicationService.PermissionRuleRecord;
import com.hjo2oa.content.category.management.application.ContentCategoryApplicationService.PermissionScope;
import com.hjo2oa.content.category.management.application.ContentCategoryApplicationService.PermissionSubjectType;
import com.hjo2oa.content.category.management.application.ContentCategoryRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class JdbcContentCategoryRepository implements ContentCategoryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcContentCategoryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<CategoryRecord> findById(UUID tenantId, UUID id) {
        String sql = """
                SELECT * FROM dbo.content_category
                WHERE tenant_id = :tenantId AND id = :id
                """;
        return jdbcTemplate.query(sql, params().addValue("tenantId", tenantId).addValue("id", id), this::mapCategory)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<CategoryRecord> findByCode(UUID tenantId, String code) {
        String sql = """
                SELECT * FROM dbo.content_category
                WHERE tenant_id = :tenantId AND code = :code
                """;
        return jdbcTemplate.query(sql, params().addValue("tenantId", tenantId).addValue("code", code), this::mapCategory)
                .stream()
                .findFirst();
    }

    @Override
    public List<CategoryRecord> findByTenantId(UUID tenantId) {
        String sql = """
                SELECT * FROM dbo.content_category
                WHERE tenant_id = :tenantId
                ORDER BY sort_order ASC, name ASC
                """;
        return jdbcTemplate.query(sql, params().addValue("tenantId", tenantId), this::mapCategory);
    }

    @Override
    public void save(CategoryRecord category) {
        String update = """
                UPDATE dbo.content_category
                SET code = :code,
                    name = :name,
                    category_type = :categoryType,
                    parent_id = :parentId,
                    route_path = :routePath,
                    sort_order = :sortOrder,
                    visible_mode = :visibleMode,
                    status = :status,
                    version_no = :versionNo,
                    updated_by = :updatedBy,
                    updated_at = :updatedAt
                WHERE tenant_id = :tenantId AND id = :id
                """;
        int rows = jdbcTemplate.update(update, params(category));
        if (rows == 0) {
            String insert = """
                    INSERT INTO dbo.content_category (
                        id, code, name, category_type, parent_id, route_path, sort_order,
                        visible_mode, status, version_no, tenant_id, created_by, updated_by,
                        created_at, updated_at
                    )
                    VALUES (
                        :id, :code, :name, :categoryType, :parentId, :routePath, :sortOrder,
                        :visibleMode, :status, :versionNo, :tenantId, :createdBy, :updatedBy,
                        :createdAt, :updatedAt
                    )
                    """;
            jdbcTemplate.update(insert, params(category));
        }
    }

    @Override
    public void replacePermissions(UUID tenantId, UUID categoryId, List<PermissionRuleRecord> rules) {
        jdbcTemplate.update(
                "DELETE FROM dbo.content_category_permission WHERE tenant_id = :tenantId AND category_id = :categoryId",
                params().addValue("tenantId", tenantId).addValue("categoryId", categoryId)
        );
        if (rules == null || rules.isEmpty()) {
            return;
        }
        String insert = """
                INSERT INTO dbo.content_category_permission (
                    id, category_id, tenant_id, subject_type, subject_id, effect,
                    scope_type, sort_order, created_at
                )
                VALUES (
                    :id, :categoryId, :tenantId, :subjectType, :subjectId, :effect,
                    :scope, :sortOrder, :createdAt
                )
                """;
        MapSqlParameterSource[] batch = rules.stream()
                .map(this::params)
                .toArray(MapSqlParameterSource[]::new);
        jdbcTemplate.batchUpdate(insert, batch);
    }

    @Override
    public List<PermissionRuleRecord> findPermissions(UUID tenantId, UUID categoryId) {
        String sql = """
                SELECT * FROM dbo.content_category_permission
                WHERE tenant_id = :tenantId AND category_id = :categoryId
                ORDER BY sort_order ASC
                """;
        return jdbcTemplate.query(
                sql,
                params().addValue("tenantId", tenantId).addValue("categoryId", categoryId),
                this::mapRule
        );
    }

    private CategoryRecord mapCategory(ResultSet rs, int rowNum) throws SQLException {
        return new CategoryRecord(
                uuid(rs, "id"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("category_type"),
                uuid(rs, "parent_id"),
                rs.getString("route_path"),
                rs.getInt("sort_order"),
                rs.getString("visible_mode"),
                CategoryStatus.valueOf(rs.getString("status")),
                rs.getInt("version_no"),
                uuid(rs, "tenant_id"),
                uuid(rs, "created_by"),
                uuid(rs, "updated_by"),
                instant(rs, "created_at"),
                instant(rs, "updated_at")
        );
    }

    private PermissionRuleRecord mapRule(ResultSet rs, int rowNum) throws SQLException {
        return new PermissionRuleRecord(
                uuid(rs, "id"),
                uuid(rs, "category_id"),
                uuid(rs, "tenant_id"),
                PermissionSubjectType.valueOf(rs.getString("subject_type")),
                uuid(rs, "subject_id"),
                PermissionEffect.valueOf(rs.getString("effect")),
                PermissionScope.valueOf(rs.getString("scope_type")),
                rs.getInt("sort_order"),
                instant(rs, "created_at")
        );
    }

    private MapSqlParameterSource params(CategoryRecord category) {
        return params()
                .addValue("id", category.id().toString(), Types.VARCHAR)
                .addValue("code", category.code())
                .addValue("name", category.name())
                .addValue("categoryType", category.categoryType())
                .addValue("parentId", uuidValue(category.parentId()), Types.VARCHAR)
                .addValue("routePath", category.routePath())
                .addValue("sortOrder", category.sortOrder())
                .addValue("visibleMode", category.visibleMode())
                .addValue("status", category.status().name())
                .addValue("versionNo", category.versionNo())
                .addValue("tenantId", category.tenantId().toString(), Types.VARCHAR)
                .addValue("createdBy", uuidValue(category.createdBy()), Types.VARCHAR)
                .addValue("updatedBy", uuidValue(category.updatedBy()), Types.VARCHAR)
                .addValue("createdAt", timestamp(category.createdAt()), Types.TIMESTAMP)
                .addValue("updatedAt", timestamp(category.updatedAt()), Types.TIMESTAMP);
    }

    private MapSqlParameterSource params(PermissionRuleRecord rule) {
        return params()
                .addValue("id", rule.id().toString(), Types.VARCHAR)
                .addValue("categoryId", rule.categoryId().toString(), Types.VARCHAR)
                .addValue("tenantId", rule.tenantId().toString(), Types.VARCHAR)
                .addValue("subjectType", rule.subjectType().name())
                .addValue("subjectId", uuidValue(rule.subjectId()), Types.VARCHAR)
                .addValue("effect", rule.effect().name())
                .addValue("scope", rule.scope().name())
                .addValue("sortOrder", rule.sortOrder())
                .addValue("createdAt", timestamp(rule.createdAt()), Types.TIMESTAMP);
    }

    private static MapSqlParameterSource params() {
        return new MapSqlParameterSource(new HashMap<>());
    }

    private static UUID uuid(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        return value == null ? null : UUID.fromString(value);
    }

    private static String uuidValue(UUID value) {
        return value == null ? null : value.toString();
    }

    private static Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        return rs.getTimestamp(column).toInstant();
    }
}
