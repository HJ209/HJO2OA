package com.hjo2oa.infra.attachment.application;

import java.nio.file.Path;

public interface AttachmentVirusScanner {

    ScanResult scan(Path path, String originalFilename, String contentType);

    record ScanResult(
            boolean clean,
            String details
    ) {

        public static ScanResult passed() {
            return new ScanResult(true, "CLEAN");
        }

        public static ScanResult rejected(String details) {
            return new ScanResult(false, details);
        }
    }
}
