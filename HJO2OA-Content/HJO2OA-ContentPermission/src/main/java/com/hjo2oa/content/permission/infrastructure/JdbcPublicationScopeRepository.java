package com.hjo2oa.content.permission.infrastructure;

import com.hjo2oa.content.permission.application.ContentPermissionApplicationService.PublicationScopeRuleRecord;
import com.hjo2oa.content.permission.application.ContentPermissionApplicationService.ScopeEffect;
import com.hjo2oa.content.permission.application.ContentPermissionApplicationService.ScopeSubjectType;
import com.hjo2oa.content.permission.application.PublicationScopeRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class JdbcPublicationScopeRepository implements PublicationScopeRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcPublicationScopeRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void replaceScopes(UUID tenantId, UUID publicationId, UUID articleId, List<PublicationScopeRuleRecord> rules) {
        jdbcTemplate.update(
                "DELETE FROM dbo.content_publication_scope WHERE tenant_id = :tenantId AND publication_id = :publicationId",
                params().addValue("tenantId", tenantId).addValue("publicationId", publicationId)
        );
        if (rules == null || rules.isEmpty()) {
            return;
        }
        String sql = """
                INSERT INTO dbo.content_publication_scope (
                    id, publication_id, article_id, tenant_id, subject_type, subject_id,
                    effect, sort_order, scope_version, created_at
                )
                VALUES (
                    :id, :publicationId, :articleId, :tenantId, :subjectType, :subjectId,
                    :effect, :sortOrder, :scopeVersion, :createdAt
                )
                """;
        jdbcTemplate.batchUpdate(sql, rules.stream().map(this::params).toArray(MapSqlParameterSource[]::new));
    }

    @Override
    public List<PublicationScopeRuleRecord> findByPublication(UUID tenantId, UUID publicationId) {
        String sql = """
                SELECT * FROM dbo.content_publication_scope
                WHERE tenant_id = :tenantId AND publication_id = :publicationId
                ORDER BY sort_order ASC
                """;
        return jdbcTemplate.query(
                sql,
                params().addValue("tenantId", tenantId).addValue("publicationId", publicationId),
                this::map
        );
    }

    @Override
    public List<PublicationScopeRuleRecord> findByArticle(UUID tenantId, UUID articleId) {
        String sql = """
                SELECT * FROM dbo.content_publication_scope
                WHERE tenant_id = :tenantId AND article_id = :articleId
                ORDER BY scope_version DESC, sort_order ASC
                """;
        return jdbcTemplate.query(
                sql,
                params().addValue("tenantId", tenantId).addValue("articleId", articleId),
                this::map
        );
    }

    @Override
    public Optional<Integer> latestScopeVersion(UUID tenantId, UUID publicationId) {
        Integer value = jdbcTemplate.queryForObject(
                """
                        SELECT MAX(scope_version) FROM dbo.content_publication_scope
                        WHERE tenant_id = :tenantId AND publication_id = :publicationId
                        """,
                params().addValue("tenantId", tenantId).addValue("publicationId", publicationId),
                Integer.class
        );
        return Optional.ofNullable(value);
    }

    private PublicationScopeRuleRecord map(ResultSet rs, int rowNum) throws SQLException {
        return new PublicationScopeRuleRecord(
                uuid(rs, "id"),
                uuid(rs, "publication_id"),
                uuid(rs, "article_id"),
                uuid(rs, "tenant_id"),
                ScopeSubjectType.valueOf(rs.getString("subject_type")),
                uuid(rs, "subject_id"),
                ScopeEffect.valueOf(rs.getString("effect")),
                rs.getInt("sort_order"),
                rs.getInt("scope_version"),
                instant(rs, "created_at")
        );
    }

    private MapSqlParameterSource params(PublicationScopeRuleRecord rule) {
        return params()
                .addValue("id", rule.id())
                .addValue("publicationId", rule.publicationId())
                .addValue("articleId", rule.articleId())
                .addValue("tenantId", rule.tenantId())
                .addValue("subjectType", rule.subjectType().name())
                .addValue("subjectId", rule.subjectId())
                .addValue("effect", rule.effect().name())
                .addValue("sortOrder", rule.sortOrder())
                .addValue("scopeVersion", rule.scopeVersion())
                .addValue("createdAt", rule.createdAt());
    }

    private static MapSqlParameterSource params() {
        return new MapSqlParameterSource(new HashMap<>());
    }

    private static UUID uuid(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        return value == null ? null : UUID.fromString(value);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        return rs.getTimestamp(column).toInstant();
    }
}
