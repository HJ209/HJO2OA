package com.hjo2oa.content.storage.application;

import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.messaging.DomainEvent;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentStorageApplicationService {

    private final ContentVersionRepository repository;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    @Autowired
    public ContentStorageApplicationService(ContentVersionRepository repository) {
        this(repository, event -> {
        }, Clock.systemUTC());
    }

    public ContentStorageApplicationService(
            ContentVersionRepository repository,
            DomainEventPublisher eventPublisher,
            Clock clock
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Transactional
    public ContentVersionView createDraft(SaveDraftCommand command) {
        validate(command.tenantId(), command.articleId(), command.editorId());
        Optional<ContentVersionRecord> existing = findRecordByIdempotencyKey(
                command.tenantId(),
                command.articleId(),
                command.idempotencyKey()
        );
        if (existing.isPresent()) {
            return toView(existing.get());
        }
        String title = requireText(command.title(), "title");
        String body = requireText(command.bodyText(), "bodyText");
        int nextVersionNo = repository.nextVersionNo(command.tenantId(), command.articleId());
        Instant now = clock.instant();
        ContentVersionRecord version = new ContentVersionRecord(
                UUID.randomUUID(),
                command.articleId(),
                command.tenantId(),
                nextVersionNo,
                title,
                command.summary(),
                defaultText(command.bodyFormat(), "MARKDOWN"),
                body,
                checksum(body),
                command.coverAttachmentId(),
                command.attachments(),
                command.tags(),
                command.editorId(),
                ContentVersionStatus.DRAFT,
                command.sourceVersionNo(),
                normalizeKey(command.idempotencyKey()),
                now,
                now
        );
        repository.save(version);
        eventPublisher.publish(new ContentVersionChangedEvent(
                UUID.randomUUID(),
                "content.version.created",
                now,
                command.tenantId().toString(),
                command.articleId(),
                nextVersionNo,
                command.editorId()
        ));
        return toView(version);
    }

    @Transactional
    public ContentVersionView markPublished(UUID tenantId, UUID articleId, int versionNo, UUID operatorId) {
        ContentVersionRecord version = requireVersion(tenantId, articleId, versionNo);
        ContentVersionRecord updated = version.withStatus(ContentVersionStatus.PUBLISHED, clock.instant());
        repository.save(updated);
        eventPublisher.publish(new ContentVersionChangedEvent(
                UUID.randomUUID(),
                "content.version.published",
                updated.updatedAt(),
                tenantId.toString(),
                articleId,
                versionNo,
                operatorId
        ));
        return toView(updated);
    }

    @Transactional
    public ContentVersionView rollback(RollbackVersionCommand command) {
        validate(command.tenantId(), command.articleId(), command.operatorId());
        Optional<ContentVersionRecord> existing = findRecordByIdempotencyKey(
                command.tenantId(),
                command.articleId(),
                command.idempotencyKey()
        );
        if (existing.isPresent()) {
            return toView(existing.get());
        }
        ContentVersionRecord source = requireVersion(command.tenantId(), command.articleId(), command.targetVersionNo());
        ContentVersionView restored = createDraft(new SaveDraftCommand(
                command.tenantId(),
                command.articleId(),
                source.title(),
                source.summary(),
                source.bodyFormat(),
                source.bodyText(),
                source.coverAttachmentId(),
                source.attachments(),
                source.tags(),
                command.operatorId(),
                source.versionNo(),
                command.idempotencyKey()
        ));
        eventPublisher.publish(new ContentVersionChangedEvent(
                UUID.randomUUID(),
                "content.version.rollback",
                clock.instant(),
                command.tenantId().toString(),
                command.articleId(),
                restored.versionNo(),
                command.operatorId()
        ));
        return restored;
    }

    @Transactional(readOnly = true)
    public ContentVersionView get(UUID tenantId, UUID articleId, int versionNo) {
        return toView(requireVersion(tenantId, articleId, versionNo));
    }

    @Transactional(readOnly = true)
    public List<ContentVersionView> versions(UUID tenantId, UUID articleId) {
        validate(tenantId, articleId, UUID.randomUUID());
        return repository.findByArticle(tenantId, articleId).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<ContentVersionView> findByIdempotencyKey(UUID tenantId, UUID articleId, String idempotencyKey) {
        return findRecordByIdempotencyKey(tenantId, articleId, idempotencyKey).map(this::toView);
    }

    @Transactional(readOnly = true)
    public ContentVersionCompareView compare(UUID tenantId, UUID articleId, int leftVersionNo, int rightVersionNo) {
        ContentVersionRecord left = requireVersion(tenantId, articleId, leftVersionNo);
        ContentVersionRecord right = requireVersion(tenantId, articleId, rightVersionNo);
        return new ContentVersionCompareView(
                toView(left),
                toView(right),
                !Objects.equals(left.title(), right.title()),
                !Objects.equals(left.summary(), right.summary()),
                !Objects.equals(left.bodyChecksum(), right.bodyChecksum()),
                added(right.tags(), left.tags()),
                added(left.tags(), right.tags()),
                right.bodyText().length() - left.bodyText().length()
        );
    }

    @Transactional(readOnly = true)
    public ContentVersionRecord requireVersion(UUID tenantId, UUID articleId, int versionNo) {
        return repository.findByVersionNo(tenantId, articleId, versionNo)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Content version not found"));
    }

    @Transactional(readOnly = true)
    public ContentVersionRecord requireLatest(UUID tenantId, UUID articleId) {
        return repository.latest(tenantId, articleId)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Content version not found"));
    }

    private ContentVersionView toView(ContentVersionRecord version) {
        return new ContentVersionView(
                version.id(),
                version.articleId(),
                version.tenantId(),
                version.versionNo(),
                version.title(),
                version.summary(),
                version.bodyFormat(),
                version.bodyText(),
                version.bodyChecksum(),
                version.coverAttachmentId(),
                version.attachments(),
                version.tags(),
                version.editorId(),
                version.status(),
                version.sourceVersionNo(),
                version.idempotencyKey(),
                version.createdAt(),
                version.updatedAt()
        );
    }

    private static List<String> added(List<String> source, List<String> baseline) {
        List<String> safeBaseline = baseline == null ? List.of() : baseline;
        return (source == null ? List.<String>of() : source)
                .stream()
                .filter(tag -> !safeBaseline.contains(tag))
                .toList();
    }

    private static void validate(UUID tenantId, UUID articleId, UUID operatorId) {
        if (tenantId == null) {
            throw new BizException(SharedErrorDescriptors.TENANT_REQUIRED, "Tenant id is required");
        }
        if (articleId == null || operatorId == null) {
            throw new BizException(SharedErrorDescriptors.VALIDATION_ERROR, "Article id and operator id are required");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BizException(SharedErrorDescriptors.VALIDATION_ERROR, fieldName + " is required");
        }
        return value.trim();
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private Optional<ContentVersionRecord> findRecordByIdempotencyKey(UUID tenantId, UUID articleId, String idempotencyKey) {
        String normalized = normalizeKey(idempotencyKey);
        if (normalized == null) {
            return Optional.empty();
        }
        return repository.findByIdempotencyKey(tenantId, articleId, normalized);
    }

    private static String normalizeKey(String idempotencyKey) {
        return idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey.trim();
    }

    private static String checksum(String body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(body.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    public enum ContentVersionStatus {
        DRAFT,
        PUBLISHED,
        ARCHIVED
    }

    public record ContentAttachment(
            UUID attachmentId,
            String fileName,
            String url,
            String contentType,
            long size
    ) {
    }

    public record ContentVersionRecord(
            UUID id,
            UUID articleId,
            UUID tenantId,
            int versionNo,
            String title,
            String summary,
            String bodyFormat,
            String bodyText,
            String bodyChecksum,
            UUID coverAttachmentId,
            List<ContentAttachment> attachments,
            List<String> tags,
            UUID editorId,
            ContentVersionStatus status,
            Integer sourceVersionNo,
            String idempotencyKey,
            Instant createdAt,
            Instant updatedAt
    ) {

        public ContentVersionRecord {
            attachments = attachments == null ? List.of() : List.copyOf(attachments);
            tags = tags == null ? List.of() : List.copyOf(tags);
        }

        ContentVersionRecord withStatus(ContentVersionStatus newStatus, Instant now) {
            return new ContentVersionRecord(
                    id,
                    articleId,
                    tenantId,
                    versionNo,
                    title,
                    summary,
                    bodyFormat,
                    bodyText,
                    bodyChecksum,
                    coverAttachmentId,
                    attachments,
                    tags,
                    editorId,
                    newStatus,
                    sourceVersionNo,
                    idempotencyKey,
                    createdAt,
                    now
            );
        }
    }

    public record SaveDraftCommand(
            UUID tenantId,
            UUID articleId,
            String title,
            String summary,
            String bodyFormat,
            String bodyText,
            UUID coverAttachmentId,
            List<ContentAttachment> attachments,
            List<String> tags,
            UUID editorId,
            Integer sourceVersionNo,
            String idempotencyKey
    ) {
    }

    public record RollbackVersionCommand(
            UUID tenantId,
            UUID articleId,
            int targetVersionNo,
            UUID operatorId,
            String idempotencyKey
    ) {
    }

    public record ContentVersionView(
            UUID id,
            UUID articleId,
            UUID tenantId,
            int versionNo,
            String title,
            String summary,
            String bodyFormat,
            String bodyText,
            String bodyChecksum,
            UUID coverAttachmentId,
            List<ContentAttachment> attachments,
            List<String> tags,
            UUID editorId,
            ContentVersionStatus status,
            Integer sourceVersionNo,
            String idempotencyKey,
            Instant createdAt,
            Instant updatedAt
    ) {

        public ContentVersionView {
            attachments = attachments == null ? List.of() : List.copyOf(attachments);
            tags = tags == null ? List.of() : List.copyOf(tags);
        }
    }

    public record ContentVersionCompareView(
            ContentVersionView left,
            ContentVersionView right,
            boolean titleChanged,
            boolean summaryChanged,
            boolean bodyChanged,
            List<String> addedTags,
            List<String> removedTags,
            int bodyLengthDelta
    ) {
    }

    public record ContentVersionChangedEvent(
            UUID eventId,
            String eventType,
            Instant occurredAt,
            String tenantId,
            UUID articleId,
            int versionNo,
            UUID operatorId
    ) implements DomainEvent {
    }
}
