package com.hjo2oa.content.statistics.infrastructure;

import com.hjo2oa.content.statistics.application.ContentStatisticsApplicationService.ContentActionRecord;
import com.hjo2oa.content.statistics.application.ContentStatisticsApplicationService.ContentActionType;
import com.hjo2oa.content.statistics.application.ContentStatisticsApplicationService.ContentEngagementSnapshot;
import com.hjo2oa.content.statistics.application.ContentStatisticsRepository;
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
public class JdbcContentStatisticsRepository implements ContentStatisticsRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcContentStatisticsRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean hasAction(UUID tenantId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(1) FROM dbo.content_read_record
                        WHERE tenant_id = :tenantId AND idempotency_key = :idempotencyKey
                        """,
                params().addValue("tenantId", tenantId).addValue("idempotencyKey", idempotencyKey),
                Integer.class
        );
        return count != null && count > 0;
    }

    @Override
    public void recordAction(ContentActionRecord record) {
        String sql = """
                INSERT INTO dbo.content_read_record (
                    id, article_id, tenant_id, person_id, assignment_id, action_type, idempotency_key, occurred_at
                )
                VALUES (
                    :id, :articleId, :tenantId, :personId, :assignmentId, :actionType, :idempotencyKey, :occurredAt
                )
                """;
        jdbcTemplate.update(sql, params(record));
    }

    @Override
    public long countActions(UUID tenantId, UUID articleId, ContentActionType actionType) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(1) FROM dbo.content_read_record
                        WHERE tenant_id = :tenantId AND article_id = :articleId AND action_type = :actionType
                        """,
                params()
                        .addValue("tenantId", tenantId)
                        .addValue("articleId", articleId)
                        .addValue("actionType", actionType.name()),
                Long.class
        );
        return count == null ? 0 : count;
    }

    @Override
    public long countUniqueReaders(UUID tenantId, UUID articleId) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(DISTINCT person_id) FROM dbo.content_read_record
                        WHERE tenant_id = :tenantId AND article_id = :articleId
                          AND action_type = 'READ' AND person_id IS NOT NULL
                        """,
                params().addValue("tenantId", tenantId).addValue("articleId", articleId),
                Long.class
        );
        return count == null ? 0 : count;
    }

    @Override
    public Optional<ContentEngagementSnapshot> findSnapshot(UUID tenantId, UUID articleId, String bucket) {
        String sql = """
                SELECT * FROM dbo.content_engagement_snapshot
                WHERE tenant_id = :tenantId AND article_id = :articleId AND stat_bucket = :bucket
                """;
        return jdbcTemplate.query(
                sql,
                params().addValue("tenantId", tenantId).addValue("articleId", articleId).addValue("bucket", bucket),
                this::mapSnapshot
        ).stream().findFirst();
    }

    @Override
    public void saveSnapshot(ContentEngagementSnapshot snapshot) {
        String update = """
                UPDATE dbo.content_engagement_snapshot
                SET read_count = :readCount,
                    unique_reader_count = :uniqueReaderCount,
                    download_count = :downloadCount,
                    favorite_count = :favoriteCount,
                    hot_score = :hotScore,
                    last_aggregated_at = :lastAggregatedAt,
                    updated_at = :updatedAt
                WHERE tenant_id = :tenantId AND article_id = :articleId AND stat_bucket = :bucket
                """;
        int rows = jdbcTemplate.update(update, params(snapshot));
        if (rows == 0) {
            String insert = """
                    INSERT INTO dbo.content_engagement_snapshot (
                        id, article_id, tenant_id, stat_bucket, read_count, unique_reader_count,
                        download_count, favorite_count, hot_score, last_aggregated_at, created_at, updated_at
                    )
                    VALUES (
                        :id, :articleId, :tenantId, :bucket, :readCount, :uniqueReaderCount,
                        :downloadCount, :favoriteCount, :hotScore, :lastAggregatedAt, :createdAt, :updatedAt
                    )
                    """;
            jdbcTemplate.update(insert, params(snapshot));
        }
    }

    @Override
    public List<ContentEngagementSnapshot> ranking(UUID tenantId, String bucket, int limit, Instant now) {
        String sql = """
                SELECT TOP (:limit) * FROM dbo.content_engagement_snapshot
                WHERE tenant_id = :tenantId AND stat_bucket = :bucket
                ORDER BY hot_score DESC, read_count DESC
                """;
        return jdbcTemplate.query(
                sql,
                params().addValue("tenantId", tenantId).addValue("bucket", bucket).addValue("limit", limit),
                this::mapSnapshot
        );
    }

    private ContentEngagementSnapshot mapSnapshot(ResultSet rs, int rowNum) throws SQLException {
        return new ContentEngagementSnapshot(
                uuid(rs, "id"),
                uuid(rs, "article_id"),
                uuid(rs, "tenant_id"),
                rs.getString("stat_bucket"),
                rs.getLong("read_count"),
                rs.getLong("unique_reader_count"),
                rs.getLong("download_count"),
                rs.getLong("favorite_count"),
                rs.getBigDecimal("hot_score"),
                instantNullable(rs, "last_aggregated_at"),
                instant(rs, "created_at"),
                instant(rs, "updated_at")
        );
    }

    private MapSqlParameterSource params(ContentActionRecord record) {
        return params()
                .addValue("id", record.id())
                .addValue("articleId", record.articleId())
                .addValue("tenantId", record.tenantId())
                .addValue("personId", record.personId())
                .addValue("assignmentId", record.assignmentId())
                .addValue("actionType", record.actionType().name())
                .addValue("idempotencyKey", record.idempotencyKey())
                .addValue("occurredAt", record.occurredAt());
    }

    private MapSqlParameterSource params(ContentEngagementSnapshot snapshot) {
        return params()
                .addValue("id", snapshot.id())
                .addValue("articleId", snapshot.articleId())
                .addValue("tenantId", snapshot.tenantId())
                .addValue("bucket", snapshot.bucket())
                .addValue("readCount", snapshot.readCount())
                .addValue("uniqueReaderCount", snapshot.uniqueReaderCount())
                .addValue("downloadCount", snapshot.downloadCount())
                .addValue("favoriteCount", snapshot.favoriteCount())
                .addValue("hotScore", snapshot.hotScore())
                .addValue("lastAggregatedAt", snapshot.lastAggregatedAt())
                .addValue("createdAt", snapshot.createdAt())
                .addValue("updatedAt", snapshot.updatedAt());
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
}
