package com.hjo2oa.content.lifecycle.infrastructure;

import com.hjo2oa.content.lifecycle.application.ContentArticleRepository;
import com.hjo2oa.content.lifecycle.application.ContentLifecycleApplicationService.ArticleListQuery;
import com.hjo2oa.content.lifecycle.application.ContentLifecycleApplicationService.ContentArticleRecord;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryContentArticleRepository implements ContentArticleRepository {

    private final Map<UUID, ContentArticleRecord> articles = new ConcurrentHashMap<>();

    @Override
    public void save(ContentArticleRecord article) {
        articles.put(article.id(), article);
    }

    @Override
    public Optional<ContentArticleRecord> findById(UUID tenantId, UUID articleId) {
        return Optional.ofNullable(articles.get(articleId)).filter(article -> article.tenantId().equals(tenantId));
    }

    @Override
    public Optional<ContentArticleRecord> findByArticleNo(UUID tenantId, String articleNo) {
        return articles.values().stream()
                .filter(article -> article.tenantId().equals(tenantId))
                .filter(article -> article.articleNo().equals(articleNo))
                .findFirst();
    }

    @Override
    public List<ContentArticleRecord> search(ArticleListQuery query) {
        String keyword = query.keyword() == null ? null : query.keyword().toLowerCase(Locale.ROOT);
        return articles.values().stream()
                .filter(article -> article.tenantId().equals(query.tenantId()))
                .filter(article -> query.categoryId() == null || article.mainCategoryId().equals(query.categoryId()))
                .filter(article -> query.status() == null || article.status() == query.status())
                .filter(article -> query.authorId() == null || article.authorId().equals(query.authorId()))
                .filter(article -> query.from() == null || !article.updatedAt().isBefore(query.from()))
                .filter(article -> query.to() == null || !article.updatedAt().isAfter(query.to()))
                .filter(article -> keyword == null || keyword.isBlank() || contains(article, keyword))
                .sorted(Comparator.comparing(ContentArticleRecord::updatedAt).reversed())
                .toList();
    }

    private static boolean contains(ContentArticleRecord article, String keyword) {
        return contains(article.title(), keyword) || contains(article.summary(), keyword);
    }

    private static boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }
}
