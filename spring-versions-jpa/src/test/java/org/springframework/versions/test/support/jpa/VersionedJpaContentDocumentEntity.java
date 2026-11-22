package org.springframework.versions.test.support.jpa;

import jakarta.persistence.*;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.versions.*;

import java.util.UUID;

/**
 * JPA entity combining content metadata and versioning for REST and store versioning ITs.
 *
 * @author Vladimir Svyatski
 */
@Entity
@Access(AccessType.FIELD)
@Table(name = "VERSIONED_DOCUMENTS")
@SuppressWarnings("unused")
public class VersionedJpaContentDocumentEntity {

    @Id
    @GeneratedValue
    private Long id;
    @Version
    private Long versionStamp;
    @ContentId
    private UUID contentId;
    @ContentLength
    private long contentLen;
    @MimeType
    private String mimeType;
    @LockOwner
    private String lockOwner;
    @AncestorId
    private Long ancestorId;
    @AncestorRootId
    private Long ancestralRootId;
    @SuccessorId
    private Long successorId;
    @VersionNumber
    private String version;
    @VersionLabel
    private String label;
    private String data;

    public VersionedJpaContentDocumentEntity() {
    }

    /**
     * Reflective entry point for {@link internal.org.springframework.versions.jpa.JpaCloningServiceImpl}.
     * {@code id} is intentionally not copied so the successor row gets a new key on {@code persist}.
     */
    @SuppressWarnings("CopyConstructorMissesField")
    public VersionedJpaContentDocumentEntity(VersionedJpaContentDocumentEntity doc) {
        this.versionStamp = doc.versionStamp;
        this.contentId = doc.contentId;
        this.contentLen = doc.contentLen;
        this.mimeType = doc.mimeType;
        this.lockOwner = doc.lockOwner;
        this.ancestorId = doc.ancestorId;
        this.ancestralRootId = doc.ancestralRootId;
        this.successorId = doc.successorId;
        this.version = doc.version;
        this.label = doc.label;
        this.data = doc.data;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getContentId() {
        return contentId;
    }

    public void setContentId(UUID contentId) {
        this.contentId = contentId;
    }

    public long getContentLen() {
        return contentLen;
    }

    public void setContentLen(long contentLen) {
        this.contentLen = contentLen;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Long getVersionStamp() {
        return versionStamp;
    }

    public void setVersionStamp(Long versionStamp) {
        this.versionStamp = versionStamp;
    }

    public String getLockOwner() {
        return lockOwner;
    }

    public void setLockOwner(String lockOwner) {
        this.lockOwner = lockOwner;
    }

    public Long getAncestorId() {
        return ancestorId;
    }

    public void setAncestorId(Long ancestorId) {
        this.ancestorId = ancestorId;
    }

    public Long getAncestralRootId() {
        return ancestralRootId;
    }

    public void setAncestralRootId(Long ancestralRootId) {
        this.ancestralRootId = ancestralRootId;
    }

    public Long getSuccessorId() {
        return successorId;
    }

    public void setSuccessorId(Long successorId) {
        this.successorId = successorId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}