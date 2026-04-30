package com.hjo2oa.infra.attachment.infrastructure;

import com.hjo2oa.infra.attachment.application.AttachmentErrorDescriptors;
import com.hjo2oa.infra.attachment.application.AttachmentStorageAdapter;
import com.hjo2oa.infra.attachment.application.StoredAttachment;
import com.hjo2oa.infra.attachment.application.StoredAttachmentResource;
import com.hjo2oa.infra.attachment.domain.StorageProvider;
import com.hjo2oa.shared.kernel.BizException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

@Component
public class LocalAttachmentStorageAdapter implements AttachmentStorageAdapter {

    private final Path rootPath;

    public LocalAttachmentStorageAdapter(
            @Value("${hjo2oa.infra.attachment.local-root:${user.dir}/target/hjo2oa-attachments}") String rootPath
    ) {
        this.rootPath = Path.of(rootPath).toAbsolutePath().normalize();
    }

    @Override
    public StorageProvider provider() {
        return StorageProvider.LOCAL;
    }

    @Override
    public StoredAttachment store(String storageKey, InputStream content) {
        Path target = resolveStorageKey(storageKey);
        try {
            Files.createDirectories(target.getParent());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long size;
            try (DigestInputStream digestInputStream = new DigestInputStream(content, digest)) {
                size = Files.copy(digestInputStream, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredAttachment(
                    storageKey,
                    size,
                    HexFormat.of().formatHex(digest.digest()),
                    StorageProvider.LOCAL,
                    target
            );
        } catch (Exception ex) {
            throw new BizException(
                    AttachmentErrorDescriptors.ATTACHMENT_STORAGE_FAILED,
                    "Failed to store attachment object",
                    ex
            );
        }
    }

    @Override
    public StoredAttachmentResource load(String storageKey) {
        Path target = resolveStorageKey(storageKey);
        if (!Files.exists(target)) {
            throw new BizException(
                    AttachmentErrorDescriptors.ATTACHMENT_NOT_FOUND,
                    "Attachment object not found: " + storageKey
            );
        }
        try {
            return new StoredAttachmentResource(new FileSystemResource(target), Files.size(target));
        } catch (Exception ex) {
            throw new BizException(
                    AttachmentErrorDescriptors.ATTACHMENT_STORAGE_FAILED,
                    "Failed to load attachment object",
                    ex
            );
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolveStorageKey(storageKey));
        } catch (Exception ex) {
            throw new BizException(
                    AttachmentErrorDescriptors.ATTACHMENT_STORAGE_FAILED,
                    "Failed to delete attachment object",
                    ex
            );
        }
    }

    private Path resolveStorageKey(String storageKey) {
        Path resolved = rootPath.resolve(storageKey).normalize();
        if (!resolved.startsWith(rootPath)) {
            throw new BizException(AttachmentErrorDescriptors.ATTACHMENT_STORAGE_FAILED, "Invalid storage key");
        }
        return resolved;
    }
}
