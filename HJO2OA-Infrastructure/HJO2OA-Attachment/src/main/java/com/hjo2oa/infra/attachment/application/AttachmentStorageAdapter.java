package com.hjo2oa.infra.attachment.application;

import com.hjo2oa.infra.attachment.domain.StorageProvider;
import java.io.InputStream;

public interface AttachmentStorageAdapter {

    StorageProvider provider();

    StoredAttachment store(String storageKey, InputStream content);

    StoredAttachmentResource load(String storageKey);

    void delete(String storageKey);
}
