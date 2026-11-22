package internal.org.springframework.content.rest.support;

import jakarta.persistence.Embeddable;
import org.hibernate.annotations.Formula;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;
import org.springframework.content.test.support.ContentAccessor;

import java.util.UUID;

@Embeddable
public class EmbeddedContentChild implements ContentAccessor<UUID> {

    @ContentId
    public UUID contentId;

    @ContentLength
    public Long contentLen;

    @MimeType
    public String contentMimeType;

    @OriginalFileName
    public String contentFileName = "";

    // prevent EmbeddedContentChild from being return by hibernate as null
    @Formula("1")
    private int workaroundForBraindeadJpaImplementation;

    public EmbeddedContentChild() {
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

    public void setContentMimeType(String mimeType) {
        this.contentMimeType = mimeType;
    }

    public String getContentFileName() {
        return contentFileName;
    }

    public void setContentFileName(String fileName) {
        this.contentFileName = fileName;
    }
}
