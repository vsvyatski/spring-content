package org.springframework.content.test.support.encryption;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.util.UUID;

/**
 * External storage for per-content encryption keys (custom {@code DataEncryptionKeyAccessor} ITs).
 *
 * @author Vladimir Svyatski
 */
@Entity
public class ContentEncryptionKey {

    @Id
    private UUID contentId;

    private String algorithm;

    private byte[] encryptionKey;

    private byte[] iv;

    public ContentEncryptionKey() {
    }

    public UUID getContentId() {
        return contentId;
    }

    public void setContentId(UUID contentId) {
        this.contentId = contentId;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public byte[] getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(byte[] encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public byte[] getIv() {
        return iv;
    }

    public void setIv(byte[] iv) {
        this.iv = iv;
    }
}