package com.hjo2oa.content.storage.infrastructure;

import com.hjo2oa.content.storage.application.ContentStorageApplicationService.ContentVersionRecord;
import com.hjo2oa.content.storage.application.ContentVersionRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryContentVersionRepository implements ContentVersionRepository {

    private final Map<String, ContentVersionRecord> versions = new ConcurrentHashMap<>();

    @Override
    public int nextVersionNo(UUID tenantId, UUID articleId) {
        return findByArticle(tenantId, articleId).stream()
                .mapToInt(ContentVersionRecord::versionNo)
                .max()
                .orElse(0) + 1;
    }

    @Override
    public void save(ContentVersionRecord version) {
        versions.put(key(version.tenantId(), version.articleId(), version.versionNo()), version);
    }

    @Override
    public Optional<ContentVersionRecord> findByVersionNo(UUID tenantId, UUID articleId, int versionNo) {
        return Optional.ofNullable(versions.get(key(tenantId, articleId, versionNo)));
    }

    @Override
    public Optional<ContentVersionRecord> findByIdempotencyKey(UUID tenantId, UUID articleId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        return versions.values().stream()
                .filter(version -> version.tenantId().equals(tenantId))
                .filter(version -> version.articleId().equals(articleId))
                .filter(version -> idempotencyKey.equals(version.idempotencyKey()))
                .findFirst();
    }

    @Override
    public Optional<ContentVersionRecord> latest(UUID tenantId, UUID articleId) {
        return findByArticle(tenantId, articleId).stream()
                .max(Comparator.comparingInt(ContentVersionRecord::versionNo));
    }

    @Override
    public List<ContentVersionRecord> findByArticle(UUID tenantId, UUID articleId) {
        return versions.values()
                .stream()
                .filter(version -> version.tenantId().equals(tenantId))
                .filter(version -> version.articleId().equals(articleId))
                .sorted(Comparator.comparingInt(ContentVersionRecord::versionNo).reversed())
                .toList();
    }

    private static String key(UUID tenantId, UUID articleId, int versionNo) {
        return tenantId + ":" + articleId + ":" + versionNo;
    }
}
