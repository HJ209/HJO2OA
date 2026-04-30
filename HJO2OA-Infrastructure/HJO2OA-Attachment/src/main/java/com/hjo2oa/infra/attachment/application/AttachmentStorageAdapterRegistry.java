package com.hjo2oa.infra.attachment.application;

import com.hjo2oa.infra.attachment.domain.StorageProvider;
import com.hjo2oa.shared.kernel.BizException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AttachmentStorageAdapterRegistry {

    private final Map<StorageProvider, AttachmentStorageAdapter> adapters = new EnumMap<>(StorageProvider.class);

    public AttachmentStorageAdapterRegistry(List<AttachmentStorageAdapter> adapters) {
        adapters.forEach(adapter -> this.adapters.put(adapter.provider(), adapter));
    }

    public AttachmentStorageAdapter require(StorageProvider provider) {
        AttachmentStorageAdapter adapter = adapters.get(provider);
        if (adapter == null) {
            throw new BizException(
                    AttachmentErrorDescriptors.ATTACHMENT_STORAGE_FAILED,
                    "Storage provider is not configured: " + provider
            );
        }
        return adapter;
    }
}
