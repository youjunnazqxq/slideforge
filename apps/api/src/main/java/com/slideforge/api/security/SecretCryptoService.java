package com.slideforge.api.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SecretCryptoService {

    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKeySpec keySpec;

    public SecretCryptoService(
            @Value("${slideforge.security.ai-key-secret:slideforge-local-development-secret}") String secret
    ) {
        this.keySpec = new SecretKeySpec(sha256(secret), "AES");
    }

    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(cipherText);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("API Key 加密失败", exception);
        }
    }

    public String decrypt(String encryptedText) {
        try {
            String[] parts = encryptedText.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("加密内容格式不正确");
            }

            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] cipherText = Base64.getDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));

            return StandardCharsets.UTF_8.decode(ByteBuffer.wrap(cipher.doFinal(cipherText))).toString();
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("API Key 解密失败", exception);
        }
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("加密密钥初始化失败", exception);
        }
    }
}

