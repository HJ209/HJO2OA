package com.hjo2oa.infra.security.infrastructure;

import com.hjo2oa.infra.security.application.CryptoResult;
import com.hjo2oa.infra.security.application.CryptoService;
import com.hjo2oa.infra.security.application.SecurityErrorDescriptors;
import com.hjo2oa.infra.security.domain.KeyStatus;
import com.hjo2oa.infra.security.domain.SecurityPolicy;
import com.hjo2oa.infra.security.domain.SecurityPolicyRepository;
import com.hjo2oa.infra.security.domain.SecurityPolicyStatus;
import com.hjo2oa.infra.security.domain.SecurityPolicyType;
import com.hjo2oa.shared.kernel.BizException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DefaultCryptoService implements CryptoService {

    private static final String AES = "AES";
    private static final String RSA = "RSA";
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_BYTES = 12;

    private final SecurityPolicyRepository securityPolicyRepository;
    private final byte[] masterSecret;
    private final SecureRandom secureRandom;
    private final Map<String, AtomicInteger> keyVersions = new ConcurrentHashMap<>();
    private final Map<String, KeyPair> rsaKeys = new ConcurrentHashMap<>();

    @Autowired
    public DefaultCryptoService(
            SecurityPolicyRepository securityPolicyRepository,
            @Value("${hjo2oa.security.crypto.master-secret:${hjo2oa.security.jwt.secret:}}") String masterSecret
    ) {
        this(securityPolicyRepository, masterSecret, new SecureRandom());
    }

    DefaultCryptoService(
            SecurityPolicyRepository securityPolicyRepository,
            String masterSecret,
            SecureRandom secureRandom
    ) {
        this.securityPolicyRepository = Objects.requireNonNull(
                securityPolicyRepository,
                "securityPolicyRepository must not be null"
        );
        this.masterSecret = requireText(masterSecret, "hjo2oa.security.crypto.master-secret")
                .getBytes(StandardCharsets.UTF_8);
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom must not be null");
    }

    @Override
    public CryptoResult encrypt(String keyRef, String algorithm, String plaintext) {
        String normalizedAlgorithm = normalizeAlgorithm(algorithm);
        String normalizedKeyRef = requireActiveKeyRef(keyRef, normalizedAlgorithm);
        String normalizedValue = Objects.requireNonNull(plaintext, "plaintext must not be null");
        try {
            int keyVersion = currentVersion(normalizedKeyRef);
            if (AES.equals(normalizedAlgorithm)) {
                return new CryptoResult(
                        normalizedKeyRef,
                        normalizedAlgorithm,
                        "v1:" + keyVersion + ":" + encryptAes(normalizedKeyRef, keyVersion, normalizedValue)
                );
            }
            return new CryptoResult(
                    normalizedKeyRef,
                    normalizedAlgorithm,
                    "v1:" + keyVersion + ":" + encryptRsa(normalizedKeyRef, keyVersion, normalizedValue)
            );
        } catch (Exception ex) {
            throw new BizException(SecurityErrorDescriptors.SECURITY_CRYPTO_FAILED, "Encrypt failed", ex);
        }
    }

    @Override
    public CryptoResult decrypt(String keyRef, String algorithm, String ciphertext) {
        String normalizedAlgorithm = normalizeAlgorithm(algorithm);
        String normalizedKeyRef = requireActiveKeyRef(keyRef, normalizedAlgorithm);
        ParsedCiphertext parsed = parseCiphertext(ciphertext);
        try {
            if (AES.equals(normalizedAlgorithm)) {
                return new CryptoResult(
                        normalizedKeyRef,
                        normalizedAlgorithm,
                        decryptAes(normalizedKeyRef, parsed.keyVersion(), parsed.payload())
                );
            }
            return new CryptoResult(
                    normalizedKeyRef,
                    normalizedAlgorithm,
                    decryptRsa(normalizedKeyRef, parsed.keyVersion(), parsed.payload())
            );
        } catch (Exception ex) {
            throw new BizException(SecurityErrorDescriptors.SECURITY_CRYPTO_FAILED, "Decrypt failed", ex);
        }
    }

    @Override
    public int rotate(String keyRef) {
        String normalizedKeyRef = requireText(keyRef, "keyRef");
        requireKnownKeyRef(normalizedKeyRef);
        return keyVersions
                .computeIfAbsent(normalizedKeyRef, ignored -> new AtomicInteger(1))
                .incrementAndGet();
    }

    private String encryptAes(String keyRef, int keyVersion, String plaintext) throws Exception {
        byte[] iv = new byte[GCM_IV_BYTES];
        secureRandom.nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey(keyRef, keyVersion), new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        byte[] payload = Arrays.copyOf(iv, iv.length + encrypted.length);
        System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
        return Base64.getEncoder().encodeToString(payload);
    }

    private String decryptAes(String keyRef, int keyVersion, String payload) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(payload);
        byte[] iv = Arrays.copyOfRange(decoded, 0, GCM_IV_BYTES);
        byte[] encrypted = Arrays.copyOfRange(decoded, GCM_IV_BYTES, decoded.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, aesKey(keyRef, keyVersion), new GCMParameterSpec(GCM_TAG_BITS, iv));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }

    private String encryptRsa(String keyRef, int keyVersion, String plaintext) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, rsaKeyPair(keyRef, keyVersion).getPublic());
        return Base64.getEncoder().encodeToString(cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)));
    }

    private String decryptRsa(String keyRef, int keyVersion, String payload) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.DECRYPT_MODE, rsaKeyPair(keyRef, keyVersion).getPrivate());
        return new String(cipher.doFinal(Base64.getDecoder().decode(payload)), StandardCharsets.UTF_8);
    }

    private SecretKey aesKey(String keyRef, int keyVersion) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(masterSecret, "HmacSHA256"));
        byte[] digest = mac.doFinal((keyRef + ":" + keyVersion).getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(Arrays.copyOf(digest, 32), AES);
    }

    private KeyPair rsaKeyPair(String keyRef, int keyVersion) {
        return rsaKeys.computeIfAbsent(keyRef + ":" + keyVersion, ignored -> {
            try {
                KeyPairGenerator generator = KeyPairGenerator.getInstance(RSA);
                generator.initialize(2048, secureRandom);
                return generator.generateKeyPair();
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to generate RSA key pair", ex);
            }
        });
    }

    private String requireActiveKeyRef(String keyRef, String algorithm) {
        String normalizedKeyRef = requireText(keyRef, "keyRef");
        requireKnownKeyRef(normalizedKeyRef);
        boolean exists = securityPolicyRepository.findAll().stream()
                .filter(policy -> policy.policyType() == SecurityPolicyType.KEY_MANAGEMENT
                        || policy.policyType() == SecurityPolicyType.SIGNATURE)
                .filter(policy -> policy.status() == SecurityPolicyStatus.ACTIVE)
                .map(SecurityPolicy::secretKeys)
                .flatMap(java.util.Collection::stream)
                .filter(key -> key.keyRef().equalsIgnoreCase(normalizedKeyRef))
                .filter(key -> key.keyStatus() == KeyStatus.ACTIVE)
                .anyMatch(key -> algorithm.equals(normalizeAlgorithm(key.algorithm())));
        if (!exists && hasAnyKeyPolicy()) {
            throw new BizException(
                    SecurityErrorDescriptors.SECURITY_SECRET_KEY_NOT_FOUND,
                    "Active key reference not found: " + normalizedKeyRef
            );
        }
        return normalizedKeyRef;
    }

    private void requireKnownKeyRef(String keyRef) {
        if (hasAnyKeyPolicy()) {
            boolean exists = securityPolicyRepository.findAll().stream()
                    .filter(policy -> policy.policyType() == SecurityPolicyType.KEY_MANAGEMENT
                            || policy.policyType() == SecurityPolicyType.SIGNATURE)
                    .flatMap(policy -> policy.secretKeys().stream())
                    .anyMatch(key -> key.keyRef().equalsIgnoreCase(keyRef));
            if (!exists) {
                throw new BizException(
                        SecurityErrorDescriptors.SECURITY_SECRET_KEY_NOT_FOUND,
                        "Key reference not found: " + keyRef
                );
            }
        }
    }

    private boolean hasAnyKeyPolicy() {
        return securityPolicyRepository.findAll().stream()
                .anyMatch(policy -> policy.policyType() == SecurityPolicyType.KEY_MANAGEMENT
                        || policy.policyType() == SecurityPolicyType.SIGNATURE);
    }

    private int currentVersion(String keyRef) {
        return keyVersions.computeIfAbsent(keyRef, ignored -> new AtomicInteger(1)).get();
    }

    private String normalizeAlgorithm(String algorithm) {
        String normalized = requireText(algorithm, "algorithm").toUpperCase(Locale.ROOT);
        if (!AES.equals(normalized) && !RSA.equals(normalized)) {
            throw new BizException(
                    SecurityErrorDescriptors.SECURITY_POLICY_RULE_VIOLATION,
                    "Unsupported crypto algorithm: " + algorithm
            );
        }
        return normalized;
    }

    private ParsedCiphertext parseCiphertext(String ciphertext) {
        String normalized = requireText(ciphertext, "ciphertext");
        String[] parts = normalized.split(":", 3);
        if (parts.length != 3 || !"v1".equals(parts[0])) {
            throw new BizException(SecurityErrorDescriptors.SECURITY_CRYPTO_FAILED, "Unsupported ciphertext format");
        }
        return new ParsedCiphertext(Integer.parseInt(parts[1]), parts[2]);
    }

    private String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private record ParsedCiphertext(
            int keyVersion,
            String payload
    ) {
    }
}
