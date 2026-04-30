package com.hjo2oa.infra.security.application;

public record CryptoResult(
        String keyRef,
        String algorithm,
        String value
) {
}
