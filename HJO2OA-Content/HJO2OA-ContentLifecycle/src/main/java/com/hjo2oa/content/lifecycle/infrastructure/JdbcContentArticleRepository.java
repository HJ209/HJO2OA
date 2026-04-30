package com.hjo2oa.content.lifecycle.infrastructure;

import com.hjo2oa.content.lifecycle.application.ContentArticleRepository;
import com.hjo2oa.content.lifecycle.application.ContentLifecycleApplicationService.ArticleListQuery;
import com.hjo2oa.content.lifecycle.application.ContentLifecycleApplicationService.ArticleStatus;
import com.hjo2oa.content.lifecycle.application.ContentLifecycleApplicationService.ContentArticleRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
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
public class JdbcContentArticleRepository implements ContentArticleRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcContentArticleRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(ContentArticleRecord article) {
        String update = """
                UPDATE dbo.content_article
                SET title = :title,
                    summary = :summary,
                    content_type = :contentType,
                    main_category_id = :mainCategoryId,
                    author_id = :authorId,
                    author_name = :authorName,
                    source_type = :sourceType,
                    source_url = :sourceUrl,
                    current_draft_version_no = :currentDraftVersionNo,
                    current_published_version_no = :currentPublishedVersionNo,
                    status = :status,
                    updated_by = :updatedBy,
                    updated_at = :updatedAt
                WHERE tenant_id = :tenantId AND id = :id
                """;
        int rows = jdbcTemplate.update(update, params(article));
        if (rows == 0) {
            String insert = """
                    INSERT INTO dbo.content_article (
                        id, article_no, title, summary, content_type, main_category_id, author_id,
                        author_name, source_type, source_url, current_draft_version_no,
                        current_published_version_no, status, tenant_id, created_by, updated_by,
                        created_at, updated_at
                    )
                    VALUES (
                        :id, :articleNo, :title, :summary, :contentType, :mainCategoryId, :authorId,
                        :authorName, :sourceType, :sourceUrl, :currentDraftVersionNo,
                        :currentPublishedVersionNo, :status, :tenantId, :createdBy, :updatedBy,
                        :createdAt, :updatedAt
                    )
                    """;
            jdbcTemplate.update(insert, params(article));
        }
    }

    @Override
    public Optional<ContentArticleRecord> findById(UUID tenantId, UUID articleId) {
        String sql = "SELECT * FROM dbo.content_article WHERE tenant_id = :tenantId AND id = :id";
        return jdbcTemplate.query(sql, params().addValue("tenantId", tenantId).addValue("id", articleId), this::map)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<ContentArticleRecord> findByArticleNo(UUID tenantId, String articleNo) {
        String sql = "SELECT * FROM dbo.content_article WHERE tenant_id = :tenantId AND article_no = :articleNo";
        return jdbcTemplate.query(
                sql,
                params().addValue("tenantId", tenantId).addValue("articleNo", articleNo),
                this::map
        ).stream().findFirst();
    }

    @Override
    public List<ContentArticleRecord> search(ArticleListQuery query) {
        List<String> predicates = new ArrayList<>();
        MapSqlParameterSource params = params().addValue("tenantId", query.tenantId());
        predicates.add("tenant_id = :tenantId");
        if (query.categoryId() != null) {
            predicates.add("main_category_id = :categoryId");
            params.addValue("categoryId", query.categoryId());
        }
        if (query.status() != null) {
            predicates.add("status = :status");
            params.addValue("status", query.status().name());
        }
        if (query.authorId() != null) {
            predicates.add("author_id = :authorId");
            params.addValue("authorId", query.authorId());
        }
        if (query.from() != null) {
            predicates.add("updated_at >= :from");
            params.addValue("from", query.from());
        }
        if (query.to() != null) {
            predicates.add("updated_at <= :to");
            params.addValue("to", query.to());
        }
        if (query.keyword() != null && !query.keyword().isBlank()) {
            predicates.add("(title LIKE :keyword OR summary LIKE :keyword)");
            params.addValue("keyword", "%" + query.keyword().trim() + "%");
        }
        String sql = "SELECT * FROM dbo.content_article WHERE "
                + String.join(" AND ", predicates)
                + " ORDER BY updated_at DESC";
        return jdbcTemplate.query(sql, params, this::map);
    }

    private ContentArticleRecord map(ResultSet rs, int rowNum) throws SQLException {
        return new ContentArticleRecord(
                uuid(rs, "id"),
                rs.getString("article_no"),
                rs.getString("title"),
                rs.getString("summary"),
                rs.getString("content_type"),
                uuid(rs, "main_category_id"),
                uuid(rs, "author_id"),
                rs.getString("author_name"),
                rs.getString("source_type"),
                rs.getString("source_url"),
                intValue(rs, "current_draft_version_no"),
                intValue(rs, "current_published_version_no"),
                ArticleStatus.valueOf(rs.getString("status")),
                uuid(rs, "tenant_id"),
                uuid(rs, "created_by"),
                uuid(rs, "updated_by"),
                instant(rs, "created_at"),
                instant(rs, "updated_at")
        );
    }

    private MapSqlParameterSource params(ContentArticleRecord article) {
        return params()
                .addValue("id", uuidValue(article.id()), Types.VARCHAR)
                .addValue("articleNo", article.articleNo())
                .addValue("title", article.title())
                .addValue("summary", article.summary(), Types.NVARCHAR)
                .addValue("contentType", article.contentType())
                .addValue("mainCategoryId", uuidValue(article.mainCategoryId()), Types.VARCHAR)
                .addValue("authorId", uuidValue(article.authorId()), Types.VARCHAR)
                .addValue("authorName", article.authorName())
                .addValue("sourceType", article.sourceType())
                .addValue("sourceUrl", article.sourceUrl(), Types.NVARCHAR)
                .addValue("currentDraftVersionNo", article.currentDraftVersionNo(), Types.INTEGER)
                .addValue("currentPublishedVersionNo", article.currentPublishedVersionNo(), Types.INTEGER)
                .addValue("status", article.status().name())
                .addValue("tenantId", uuidValue(article.tenantId()), Types.VARCHAR)
                .addValue("createdBy", uuidValue(article.createdBy()), Types.VARCHAR)
                .addValue("updatedBy", uuidValue(article.updatedBy()), Types.VARCHAR)
                .addValue("createdAt", timestamp(article.createdAt()), Types.TIMESTAMP)
                .addValue("updatedAt", timestamp(article.updatedAt()), Types.TIMESTAMP);
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
