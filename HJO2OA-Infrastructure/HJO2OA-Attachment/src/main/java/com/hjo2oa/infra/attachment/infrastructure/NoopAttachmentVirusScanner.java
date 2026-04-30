package com.hjo2oa.infra.attachment.infrastructure;

import com.hjo2oa.infra.attachment.application.AttachmentVirusScanner;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

@Component
public class NoopAttachmentVirusScanner implements AttachmentVirusScanner {

    @Override
    public ScanResult scan(Path path, String originalFilename, String contentType) {
        return ScanResult.passed();
    }
}
