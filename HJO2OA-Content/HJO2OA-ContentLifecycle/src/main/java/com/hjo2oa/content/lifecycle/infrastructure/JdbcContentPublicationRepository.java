package com.hjo2oa.content.lifecycle.infrastructure;

import com.hjo2oa.content.lifecycle.application.ContentLifecycleApplicationService.ContentPublicationRecord;
import com.hjo2oa.content.lifecycle.application.ContentLifecycleApplicationService.ContentReviewRecord;
import com.hjo2oa.content.lifecycle.application.ContentLifecycleApplicationService.PublicationStatus;
import com.hjo2oa.content.lifecycle.application.ContentLifecycleApplicationService.ReviewAction;
import com.hjo2oa.content.lifecycle.application.ContentLifecycleApplicationService.ReviewMode;
import com.hjo2oa.content.lifecycle.application.ContentLifecycleApplicationService.ReviewStatus;
import com.hjo2oa.content.lifecycle.application.ContentPublicationRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
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
public class JdbcContentPublicationRepository implements ContentPublicationRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcContentPublicationRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(ContentPublicationRecord publication) {
        String update = """
                UPDATE dbo.content_publication
                SET target_version_no = :targetVersionNo,
                    review_mode = :reviewMode,
                    review_status = :reviewStatus,
                    workflow_instance_id = :workflowInstanceId,
                    publication_status = :publicationStatus,
                    start_at = :startAt,
                    end_at = :endAt,
                    published_at = :publishedAt,
                    published_by = :publishedBy,
                    offline_at = :offlineAt,
                    offline_by = :offlineBy,
                    archive_at = :archiveAt,
                    archived_by = :archivedBy,
                    reason = :reason,
                    updated_at = :updatedAt
                WHERE tenant_id = :tenantId AND id = :id
                """;
        int rows = jdbcTemplate.update(update, params(publication));
        if (rows == 0) {
            String insert = """
                    INSERT INTO dbo.content_publication (
                        id, article_id, tenant_id, target_version_no, review_mode, review_status,
                        workflow_instance_id, publication_status, start_at, end_at, published_at,
                        published_by, offline_at, offline_by, archive_at, archived_by, reason,
                        created_at, updated_at
                    )
                    VALUES (
                        :id, :articleId, :tenantId, :targetVersionNo, :reviewMode, :reviewStatus,
                        :workflowInstanceId, :publicationStatus, :startAt, :endAt, :publishedAt,
                        :publishedBy, :offlineAt, :offlineBy, :archiveAt, :archivedBy, :reason,
                        :createdAt, :updatedAt
                    )
                    """;
            jdbcTemplate.update(insert, params(publication));
        }
    }

    @Override
    public Optional<ContentPublicationRecord> findById(UUID tenantId, UUID publicationId) {
        String sql = "SELECT * FROM dbo.content_publication WHERE tenant_id = :tenantId AND id = :id";
        return jdbcTemplate.query(sql, params().addValue("tenantId", tenantId).addValue("id", publicationId), this::mapPublication)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<ContentPublicationRecord> findActiveByArticle(UUID tenantId, UUID articleId) {
        String sql = """
                SELECT TOP 1 * FROM dbo.content_publication
                WHERE tenant_id = :tenantId AND article_id = :articleId AND publication_status = 'PUBLISHED'
                ORDER BY published_at DESC
                """;
        return jdbcTemplate.query(
                sql,
                params().addValue("tenantId", tenantId).addValue("articleId", articleId),
                this::mapPublication
        ).stream().findFirst();
    }

    @Override
    public Optional<ContentPublicationRecord> findPendingReview(UUID tenantId, UUID articleId, int versionNo) {
        String sql = """
                SELECT TOP 1 * FROM dbo.content_publication
                WHERE tenant_id = :tenantId AND article_id = :articleId
                  AND target_version_no = :versionNo AND review_mode = 'REVIEW'
                  AND publication_status IN ('PENDING_REVIEW', 'REJECTED')
                ORDER BY updated_at DESC
                """;
        return jdbcTemplate.query(
                sql,
                params().addValue("tenantId", tenantId).addValue("articleId", articleId).addValue("versionNo", versionNo),
                this::mapPublication
        ).stream().findFirst();
    }

    @Override
    public List<ContentPublicationRecord> findByArticle(UUID tenantId, UUID articleId) {
        String sql = """
                SELECT * FROM dbo.content_publication
                WHERE tenant_id = :tenantId AND article_id = :articleId
                ORDER BY updated_at DESC
                """;
        return jdbcTemplate.query(
                sql,
                params().addValue("tenantId", tenantId).addValue("articleId", articleId),
                this::mapPublication
        );
    }

    @Override
    public void appendReview(ContentReviewRecord record) {
        String sql = """
                INSERT INTO dbo.content_review_record (
                    id, publication_id, article_id, tenant_id, action, operator_id, opinion, created_at
                )
                VALUES (
                    :id, :publicationId, :articleId, :tenantId, :action, :operatorId, :opinion, :createdAt
                )
                """;
        jdbcTemplate.update(sql, params(record));
    }

    @Override
    public List<ContentReviewRecord> reviews(UUID tenantId, UUID articleId) {
        String sql = """
                SELECT * FROM dbo.content_review_record
                WHERE tenant_id = :tenantId AND article_id = :articleId
                ORDER BY created_at ASC
                """;
        return jdbcTemplate.query(
                sql,
                params().addValue("tenantId", tenantId).addValue("articleId", articleId),
                this::mapReview
        );
    }

    private ContentPublicationRecord mapPublication(ResultSet rs, int rowNum) throws SQLException {
        return new ContentPublicationRecord(
                uuid(rs, "id"),
                uuid(rs, "article_id"),
                uuid(rs, "tenant_id"),
                rs.getInt("target_version_no"),
                ReviewMode.valueOf(rs.getString("review_mode")),
                ReviewStatus.valueOf(rs.getString("review_status")),
                uuid(rs, "workflow_instance_id"),
                PublicationStatus.valueOf(rs.getString("publication_status")),
                instantNullable(rs, "start_at"),
                instantNullable(rs, "end_at"),
                instantNullable(rs, "published_at"),
                uuid(rs, "published_by"),
                instantNullable(rs, "offline_at"),
                uuid(rs, "offline_by"),
                instantNullable(rs, "archive_at"),
                uuid(rs, "archived_by"),
                rs.getString("reason"),
                instant(rs, "created_at"),
                instant(rs, "updated_at")
        );
    }

    private ContentReviewRecord mapReview(ResultSet rs, int rowNum) throws SQLException {
        return new ContentReviewRecord(
                uuid(rs, "id"),
                uuid(rs, "publication_id"),
                uuid(rs, "article_id"),
                uuid(rs, "tenant_id"),
                ReviewAction.valueOf(rs.getString("action")),
                uuid(rs, "operator_id"),
                rs.getString("opinion"),
                instant(rs, "created_at")
        );
    }

    private MapSqlParameterSource params(ContentPublicationRecord publication) {
        return params()
                .addValue("id", uuidValue(publication.id()), Types.VARCHAR)
                .addValue("articleId", uuidValue(publication.articleId()), Types.VARCHAR)
                .addValue("tenantId", uuidValue(publication.tenantId()), Types.VARCHAR)
                .addValue("targetVersionNo", publication.targetVersionNo())
                .addValue("reviewMode", publication.reviewMode().name())
                .addValue("reviewStatus", publication.reviewStatus().name())
                .addValue("workflowInstanceId", uuidValue(publication.workflowInstanceId()), Types.VARCHAR)
                .addValue("publicationStatus", publication.publicationStatus().name())
                .addValue("startAt", timestamp(publication.startAt()), Types.TIMESTAMP)
                .addValue("endAt", timestamp(publication.endAt()), Types.TIMESTAMP)
                .addValue("publishedAt", timestamp(publication.publishedAt()), Types.TIMESTAMP)
                .addValue("publishedBy", uuidValue(publication.publishedBy()), Types.VARCHAR)
                .addValue("offlineAt", timestamp(publication.offlineAt()), Types.TIMESTAMP)
                .addValue("offlineBy", uuidValue(publication.offlineBy()), Types.VARCHAR)
                .addValue("archiveAt", timestamp(publication.archiveAt()), Types.TIMESTAMP)
                .addValue("archivedBy", uuidValue(publication.archivedBy()), Types.VARCHAR)
                .addValue("reason", publication.reason(), Types.NVARCHAR)
                .addValue("createdAt", timestamp(publication.createdAt()), Types.TIMESTAMP)
                .addValue("updatedAt", timestamp(publication.updatedAt()), Types.TIMESTAMP);
    }

    private MapSqlParameterSource params(ContentReviewRecord record) {
        return params()
                .addValue("id", uuidValue(record.id()), Types.VARCHAR)
                .addValue("publicationId", uuidValue(record.publicationId()), Types.VARCHAR)
                .addValue("articleId", uuidValue(record.articleId()), Types.VARCHAR)
                .addValue("tenantId", uuidValue(record.tenantId()), Types.VARCHAR)
                .addValue("action", record.action().name())
                .addValue("operatorId", uuidValue(record.operatorId()), Types.VARCHAR)
                .addValue("opinion", record.opinion(), Types.NVARCHAR)
                .addValue("createdAt", timestamp(record.createdAt()), Types.TIMESTAMP);
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

    private static Instant instantNullable(ResultSet rs, String column) throws SQLException {
        java.sql.Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private static String uuidValue(UUID value) {
        return value == null ? null : value.toString();
    }

    private static Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }
}
