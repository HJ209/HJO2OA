package com.hjo2oa.content.search.infrastructure;

import com.hjo2oa.content.search.application.ContentSearchApplicationService.ContentSearchCriteria;
import com.hjo2oa.content.search.application.ContentSearchApplicationService.ContentSearchDocument;
import com.hjo2oa.content.search.application.ContentSearchIndexRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryContentSearchIndexRepository implements ContentSearchIndexRepository {

    private final Map<String, ContentSearchDocument> documents = new ConcurrentHashMap<>();

    @Override
    public void save(ContentSearchDocument document) {
        documents.put(key(document.tenantId(), document.articleId()), document);
    }

    @Override
    public void remove(UUID tenantId, UUID articleId) {
        documents.remove(key(tenantId, articleId));
    }

    @Override
    public Optional<ContentSearchDocument> findByArticleId(UUID tenantId, UUID articleId) {
        return Optional.ofNullable(documents.get(key(tenantId, articleId)));
    }

    @Override
    public List<ContentSearchDocument> search(ContentSearchCriteria criteria) {
        String keyword = criteria.keyword() == null ? null : criteria.keyword().toLowerCase(Locale.ROOT);
        return documents.values().stream()
                .filter(document -> document.tenantId().equals(criteria.tenantId()))
                .filter(document -> criteria.status() == null || document.status() == criteria.status())
                .filter(document -> criteria.categoryId() == null || document.categoryId().equals(criteria.categoryId()))
                .filter(document -> criteria.authorId() == null || document.authorId().equals(criteria.authorId()))
                .filter(document -> criteria.publishedFrom() == null || !document.publishedAt().isBefore(criteria.publishedFrom()))
                .filter(document -> criteria.publishedTo() == null || !document.publishedAt().isAfter(criteria.publishedTo()))
                .filter(document -> keyword == null || keyword.isBlank() || containsKeyword(document, keyword))
                .sorted(Comparator.comparing(ContentSearchDocument::publishedAt).reversed())
                .toList();
    }

    private static boolean containsKeyword(ContentSearchDocument document, String keyword) {
        return contains(document.title(), keyword)
                || contains(document.summary(), keyword)
                || contains(document.bodyText(), keyword);
    }

    private static boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private static String key(UUID tenantId, UUID articleId) {
        return tenantId + ":" + articleId;
    }
}
