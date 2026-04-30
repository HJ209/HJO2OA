package com.hjo2oa.content.search.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.content.search.application.ContentSearchApplicationService.ContentSearchCriteria;
import com.hjo2oa.content.search.application.ContentSearchApplicationService.ContentSearchDocument;
import com.hjo2oa.content.search.application.ContentSearchApplicationService.SearchDocumentStatus;
import com.hjo2oa.content.search.application.ContentSearchApplicationService.VisibilityRule;
import com.hjo2oa.content.search.application.ContentSearchIndexRepository;
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
public class JdbcContentSearchIndexRepository implements ContentSearchIndexRepository {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<VisibilityRule>> VISIBILITY_LIST = new TypeReference<>() {
    };

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcContentSearchIndexRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(ContentSearchDocument document) {
        String update = """
                UPDATE dbo.content_search_index
                SET publication_id = :publicationId,
                    category_id = :categoryId,
                    title = :title,
                    summary = :summary,
                    body_text = :bodyText,
                    author_id = :authorId,
                    author_name = :authorName,
                    tags_json = :tagsJson,
                    status = :status,
                    published_at = :publishedAt,
                    updated_at = :updatedAt,
                    visible_scope_json = :visibleScopeJson,
                    hot_score = :hotScore
                WHERE tenant_id = :tenantId AND article_id = :articleId
                """;
        int rows = jdbcTemplate.update(update, params(document));
        if (rows == 0) {
            String insert = """
                    INSERT INTO dbo.content_search_index (
                        article_id, publication_id, tenant_id, category_id, title, summary, body_text,
                        author_id, author_name, tags_json, status, published_at, updated_at,
                        visible_scope_json, hot_score
                    )
                    VALUES (
                        :articleId, :publicationId, :tenantId, :categoryId, :title, :summary, :bodyText,
                        :authorId, :authorName, :tagsJson, :status, :publishedAt, :updatedAt,
                        :visibleScopeJson, :hotScore
                    )
                    """;
            jdbcTemplate.update(insert, params(document));
        }
    }

    @Override
    public void remove(UUID tenantId, UUID articleId) {
        jdbcTemplate.update(
                "DELETE FROM dbo.content_search_index WHERE tenant_id = :tenantId AND article_id = :articleId",
                params().addValue("tenantId", tenantId).addValue("articleId", articleId)
        );
    }

    @Override
    public Optional<ContentSearchDocument> findByArticleId(UUID tenantId, UUID articleId) {
        String sql = """
                SELECT * FROM dbo.content_search_index
                WHERE tenant_id = :tenantId AND article_id = :articleId
                """;
        return jdbcTemplate.query(
                sql,
                params().addValue("tenantId", tenantId).addValue("articleId", articleId),
                this::map
        ).stream().findFirst();
    }

    @Override
    public List<ContentSearchDocument> search(ContentSearchCriteria criteria) {
        List<String> predicates = new ArrayList<>();
        predicates.add("tenant_id = :tenantId");
        MapSqlParameterSource params = params().addValue("tenantId", criteria.tenantId());
        if (criteria.status() != null) {
            predicates.add("status = :status");
            params.addValue("status", criteria.status().name());
        }
        if (criteria.categoryId() != null) {
            predicates.add("category_id = :categoryId");
            params.addValue("categoryId", criteria.categoryId());
        }
        if (criteria.authorId() != null) {
            predicates.add("author_id = :authorId");
            params.addValue("authorId", criteria.authorId());
        }
        if (criteria.publishedFrom() != null) {
            predicates.add("published_at >= :publishedFrom");
            params.addValue("publishedFrom", criteria.publishedFrom());
        }
        if (criteria.publishedTo() != null) {
            predicates.add("published_at <= :publishedTo");
            params.addValue("publishedTo", criteria.publishedTo());
        }
        if (criteria.keyword() != null && !criteria.keyword().isBlank()) {
            predicates.add("(title LIKE :keyword OR summary LIKE :keyword OR body_text LIKE :keyword)");
            params.addValue("keyword", "%" + criteria.keyword().trim() + "%");
        }
        String sql = "SELECT * FROM dbo.content_search_index WHERE "
                + String.join(" AND ", predicates)
                + " ORDER BY published_at DESC";
        return jdbcTemplate.query(sql, params, this::map);
    }

    private ContentSearchDocument map(ResultSet rs, int rowNum) throws SQLException {
        return new ContentSearchDocument(
                uuid(rs, "article_id"),
                uuid(rs, "publication_id"),
                uuid(rs, "tenant_id"),
                uuid(rs, "category_id"),
                rs.getString("title"),
                rs.getString("summary"),
                rs.getString("body_text"),
                uuid(rs, "author_id"),
                rs.getString("author_name"),
                read(rs.getString("tags_json"), STRING_LIST),
                SearchDocumentStatus.valueOf(rs.getString("status")),
                instant(rs, "published_at"),
                instant(rs, "updated_at"),
                read(rs.getString("visible_scope_json"), VISIBILITY_LIST),
                rs.getBigDecimal("hot_score")
        );
    }

    private MapSqlParameterSource params(ContentSearchDocument document) {
        return params()
                .addValue("articleId", uuidValue(document.articleId()), Types.VARCHAR)
                .addValue("publicationId", uuidValue(document.publicationId()), Types.VARCHAR)
                .addValue("tenantId", uuidValue(document.tenantId()), Types.VARCHAR)
                .addValue("categoryId", uuidValue(document.categoryId()), Types.VARCHAR)
                .addValue("title", document.title())
                .addValue("summary", document.summary(), Types.NVARCHAR)
                .addValue("bodyText", document.bodyText())
                .addValue("authorId", uuidValue(document.authorId()), Types.VARCHAR)
                .addValue("authorName", document.authorName())
                .addValue("tagsJson", write(document.tags()))
                .addValue("status", document.status().name())
                .addValue("publishedAt", timestamp(document.publishedAt()), Types.TIMESTAMP)
                .addValue("updatedAt", timestamp(document.updatedAt()), Types.TIMESTAMP)
                .addValue("visibleScopeJson", write(document.visibilityRules()))
                .addValue("hotScore", document.hotScore());
    }

    private <T> T read(String json, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(json == null || json.isBlank() ? "[]" : json, typeReference);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Invalid content search JSON", ex);
        }
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to write content search JSON", ex);
        }
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

    private static String uuidValue(UUID value) {
        return value == null ? null : value.toString();
    }

    private static Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }
}
