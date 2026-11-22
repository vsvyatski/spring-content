package internal.org.springframework.content.rest.support;

import jakarta.persistence.Embeddable;
import org.hibernate.annotations.Formula;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;
import org.springframework.content.test.support.UuidDualContentAccessor;

import java.util.UUID;

/**
 * Nested dual content with {@code preview*} property names (REST property paths); implements {@link UuidDualContentAccessor}
 * via those fields.
 */
@Embeddable
public class DualContentEmbeddedChild implements UuidDualContentAccessor {

    @ContentId
    public UUID contentId;

    @ContentLength
    public Long contentLen;

    @MimeType
    public String contentMimeType;

    @OriginalFileName
    public String contentFileName = "";

    @ContentId
    public UUID previewId;

    @ContentLength
    public Long previewLen;

    @MimeType
    public String previewMimeType;

    // prevent EmbeddedContentChild from being return by hibernate as null
    @Formula("1")
    @SuppressWarnings("unused")
    private int workaroundForBraindeadJpaImplementation;

    public DualContentEmbeddedChild() {
    }

    @Override
    public UUID getContentId() {
        return contentId;
    }

    @Override
    public void setContentId(UUID contentId) {
        this.contentId = contentId;
    }

    @Override
    public Long getContentLen() {
        return contentLen;
    }

    @Override
    public void setContentLen(Long contentLen) {
        this.contentLen = contentLen;
    }

    public String getContentMimeType() {
        return contentMimeType;
    }

    public void setContentMimeType(String contentMimeType) {
        this.contentMimeType = contentMimeType;
    }

    public void setContentFileName(String contentFileName) {
        this.contentFileName = contentFileName;
    }

    public UUID getPreviewId() {
        return previewId;
    }

    public Long getPreviewLen() {
        return previewLen;
    }

    public String getPreviewMimeType() {
        return previewMimeType;
    }

    public void setPreviewMimeType(String previewMimeType) {
        this.previewMimeType = previewMimeType;
    }

    @Override
    public UUID getRenditionId() {
        return previewId;
    }

    @Override
    public void setRenditionId(UUID renditionId) {
        this.previewId = renditionId;
    }

    @Override
    public Long getRenditionLen() {
        return previewLen;
    }

    @Override
    public void setRenditionLen(Long renditionLen) {
        this.previewLen = renditionLen;
    }
}
