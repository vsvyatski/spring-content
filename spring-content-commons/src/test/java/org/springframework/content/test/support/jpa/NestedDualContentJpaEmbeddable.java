package org.springframework.content.test.support.jpa;

import jakarta.persistence.Embeddable;
import org.hibernate.annotations.Formula;
import org.jspecify.annotations.Nullable;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.test.support.DualContentAccessor;

/**
 * Embedded dual content + MIME for {@code claimForm/*} property-path JPA store ITs.
 * <p>
 * Fields are declared on this {@link Embeddable} type (not inherited from a non-embeddable superclass) so Hibernate
 * maps content metadata columns reliably across dialects.
 *
 * @author Vladimir Svyatski
 */
@Embeddable
public class NestedDualContentJpaEmbeddable implements DualContentAccessor {

    @ContentId
    private String contentId;

    @ContentLength
    private Long contentLength;

    @MimeType
    private String contentMimeType = "text/plain";

    @ContentId
    private String renditionId;

    @ContentLength
    private long renditionLen;

    @Formula("1")
    @SuppressWarnings("unused")
    private int workaroundForBraindeadJpaImplementation;

    public NestedDualContentJpaEmbeddable() {
    }

    @Override
    public String getContentId() {
        return contentId;
    }

    @Override
    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    public @Nullable Long getContentLength() {
        return contentLength;
    }

    public void setContentLength(@Nullable Long contentLength) {
        this.contentLength = contentLength;
    }

    public String getContentMimeType() {
        return contentMimeType;
    }

    public void setContentMimeType(String contentMimeType) {
        this.contentMimeType = contentMimeType;
    }

    @Override
    public @Nullable Long getContentLen() {
        return contentLength;
    }

    @Override
    public void setContentLen(@Nullable Long contentLen) {
        this.contentLength = contentLen;
    }

    @Override
    public String getRenditionId() {
        return renditionId;
    }

    @Override
    public void setRenditionId(String renditionId) {
        this.renditionId = renditionId;
    }

    @Override
    public Long getRenditionLen() {
        return renditionLen;
    }

    @Override
    public void setRenditionLen(Long renditionLen) {
        this.renditionLen = renditionLen != null ? renditionLen : 0L;
    }
}
