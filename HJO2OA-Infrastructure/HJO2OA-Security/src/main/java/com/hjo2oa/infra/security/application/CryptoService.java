package com.hjo2oa.infra.security.application;

public interface CryptoService {

    CryptoResult encrypt(String keyRef, String algorithm, String plaintext);

    CryptoResult decrypt(String keyRef, String algorithm, String ciphertext);

    int rotate(String keyRef);
}
