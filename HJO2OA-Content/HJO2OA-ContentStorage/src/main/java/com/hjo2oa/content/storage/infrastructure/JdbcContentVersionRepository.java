package com.hjo2oa.content.storage.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.content.storage.application.ContentStorageApplicationService.ContentAttachment;
import com.hjo2oa.content.storage.application.ContentStorageApplicationService.ContentVersionRecord;
import com.hjo2oa.content.storage.application.ContentStorageApplicationService.ContentVersionStatus;
import com.hjo2oa.content.storage.application.ContentVersionRepository;
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
public class JdbcContentVersionRepository implements ContentVersionRepository {

    private static final TypeReference<List<ContentAttachment>> ATTACHMENT_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcContentVersionRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public int nextVersionNo(UUID tenantId, UUID articleId) {
        Integer value = jdbcTemplate.queryForObject(
                """
                        SELECT COALESCE(MAX(version_no), 0) + 1
                        FROM dbo.content_article_version
                        WHERE tenant_id = :tenantId AND article_id = :articleId
                        """,
                params().addValue("tenantId", tenantId).addValue("articleId", articleId),
                Integer.class
        );
        return value == null ? 1 : value;
    }

    @Override
    public void save(ContentVersionRecord version) {
        String update = """
                UPDATE dbo.content_article_version
                SET title_snapshot = :title,
                    summary_snapshot = :summary,
                    body_format = :bodyFormat,
                    body_text = :bodyText,
                    body_checksum = :bodyChecksum,
                    cover_attachment_id = :coverAttachmentId,
                    attachments_json = :attachmentsJson,
                    tags_json = :tagsJson,
                    editor_id = :editorId,
                    status = :status,
                    source_version_no = :sourceVersionNo,
                    idempotency_key = COALESCE(idempotency_key, :idempotencyKey),
                    updated_at = :updatedAt
                WHERE tenant_id = :tenantId AND article_id = :articleId AND version_no = :versionNo
                """;
        int rows = jdbcTemplate.update(update, params(version));
        if (rows == 0) {
            String insert = """
                    INSERT INTO dbo.content_article_version (
                        id, article_id, tenant_id, version_no, title_snapshot, summary_snapshot,
                        body_format, body_text, body_checksum, cover_attachment_id, attachments_json,
                        tags_json, editor_id, status, source_version_no, idempotency_key, created_at, updated_at
                    )
                    VALUES (
                        :id, :articleId, :tenantId, :versionNo, :title, :summary,
                        :bodyFormat, :bodyText, :bodyChecksum, :coverAttachmentId, :attachmentsJson,
                        :tagsJson, :editorId, :status, :sourceVersionNo, :idempotencyKey, :createdAt, :updatedAt
                    )
                    """;
            jdbcTemplate.update(insert, params(version));
        }
    }

    @Override
    public Optional<ContentVersionRecord> findByVersionNo(UUID tenantId, UUID articleId, int versionNo) {
        String sql = """
                SELECT * FROM dbo.content_article_version
                WHERE tenant_id = :tenantId AND article_id = :articleId AND version_no = :versionNo
                """;
        return jdbcTemplate.query(
                sql,
                params().addValue("tenantId", tenantId).addValue("articleId", articleId).addValue("versionNo", versionNo),
                this::map
        ).stream().findFirst();
    }

    @Override
    public Optional<ContentVersionRecord> findByIdempotencyKey(UUID tenantId, UUID articleId, String idempotencyKey) {
        String sql = """
                SELECT TOP 1 * FROM dbo.content_article_version
                WHERE tenant_id = :tenantId AND article_id = :articleId AND idempotency_key = :idempotencyKey
                ORDER BY version_no DESC
                """;
        return jdbcTemplate.query(
                sql,
                params()
                        .addValue("tenantId", tenantId)
                        .addValue("articleId", articleId)
                        .addValue("idempotencyKey", idempotencyKey),
                this::map
        ).stream().findFirst();
    }

    @Override
    public Optional<ContentVersionRecord> latest(UUID tenantId, UUID articleId) {
        String sql = """
                SELECT TOP 1 * FROM dbo.content_article_version
                WHERE tenant_id = :tenantId AND article_id = :articleId
                ORDER BY version_no DESC
                """;
        return jdbcTemplate.query(
                sql,
                params().addValue("tenantId", tenantId).addValue("articleId", articleId),
                this::map
        ).stream().findFirst();
    }

    @Override
    public List<ContentVersionRecord> findByArticle(UUID tenantId, UUID articleId) {
        String sql = """
                SELECT * FROM dbo.content_article_version
                WHERE tenant_id = :tenantId AND article_id = :articleId
                ORDER BY version_no DESC
                """;
        return jdbcTemplate.query(
                sql,
                params().addValue("tenantId", tenantId).addValue("articleId", articleId),
                this::map
        );
    }

    private ContentVersionRecord map(ResultSet rs, int rowNum) throws SQLException {
        return new ContentVersionRecord(
                uuid(rs, "id"),
                uuid(rs, "article_id"),
                uuid(rs, "tenant_id"),
                rs.getInt("version_no"),
                rs.getString("title_snapshot"),
                rs.getString("summary_snapshot"),
                rs.getString("body_format"),
                rs.getString("body_text"),
                rs.getString("body_checksum"),
                uuid(rs, "cover_attachment_id"),
                read(rs.getString("attachments_json"), ATTACHMENT_LIST),
                read(rs.getString("tags_json"), STRING_LIST),
                uuid(rs, "editor_id"),
                ContentVersionStatus.valueOf(rs.getString("status")),
                intValue(rs, "source_version_no"),
                rs.getString("idempotency_key"),
                instant(rs, "created_at"),
                instant(rs, "updated_at")
        );
    }

    private MapSqlParameterSource params(ContentVersionRecord version) {
        return params()
                .addValue("id", uuidValue(version.id()), Types.VARCHAR)
                .addValue("articleId", uuidValue(version.articleId()), Types.VARCHAR)
                .addValue("tenantId", uuidValue(version.tenantId()), Types.VARCHAR)
                .addValue("versionNo", version.versionNo())
                .addValue("title", version.title())
                .addValue("summary", version.summary(), Types.NVARCHAR)
                .addValue("bodyFormat", version.bodyFormat())
                .addValue("bodyText", version.bodyText())
                .addValue("bodyChecksum", version.bodyChecksum())
                .addValue("coverAttachmentId", uuidValue(version.coverAttachmentId()), Types.VARCHAR)
                .addValue("attachmentsJson", write(version.attachments()))
                .addValue("tagsJson", write(version.tags()))
                .addValue("editorId", uuidValue(version.editorId()), Types.VARCHAR)
                .addValue("status", version.status().name())
                .addValue("sourceVersionNo", version.sourceVersionNo(), Types.INTEGER)
                .addValue("idempotencyKey", version.idempotencyKey(), Types.NVARCHAR)
                .addValue("createdAt", timestamp(version.createdAt()), Types.TIMESTAMP)
                .addValue("updatedAt", timestamp(version.updatedAt()), Types.TIMESTAMP);
    }

    private <T> T read(String json, TypeReference<T> typeReference) {
        if (json == null || json.isBlank()) {
            try {
                return objectMapper.readValue("[]", typeReference);
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException("Unable to read empty JSON array", ex);
            }
        }
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Invalid content version JSON", ex);
        }
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to write content version JSON", ex);
        }
    }

    private static MapSqlParameterSource params() {
        return new MapSqlParameterSource(new HashMap<>());
    }

    private static UUID uuid(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        return value == null ? null : UUID.fromString(value);
    }

    private static Integer intValue(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        return rs.getTimestamp(column).toInstant();
    }

    private static String uuidValue(UUID value) {
        return value == null ? null : value.toString();
    }

    private static Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }
}
