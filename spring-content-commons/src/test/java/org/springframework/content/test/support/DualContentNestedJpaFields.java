package org.springframework.content.test.support;

import org.jspecify.annotations.Nullable;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;

/**
 * Dual content field names used by nested JPA embeddable ITs ({@code contentLength}, primitive {@code renditionLen}).
 *
 * @author Vladimir Svyatski
 */
public class DualContentNestedJpaFields implements DualContentAccessor {

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

    public DualContentNestedJpaFields() {
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
