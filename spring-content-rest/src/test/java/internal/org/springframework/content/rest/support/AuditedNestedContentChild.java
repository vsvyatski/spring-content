package internal.org.springframework.content.rest.support;

import jakarta.persistence.Embeddable;
import org.hibernate.annotations.Formula;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.test.support.ContentAccessor;

/**
 * Nested content metadata for {@link AuditedNestedContentEntity}.
 *
 * @author Vladimir Svyatski
 */
@Embeddable
public class AuditedNestedContentChild implements ContentAccessor<String> {

    @ContentId
    private String contentId;

    @ContentLength
    private Long contentLen;

    @MimeType
    private String mimeType;

    @Formula("1")
    @SuppressWarnings("unused")
    private int workaroundForBraindeadJpaImplementation;

    public AuditedNestedContentChild() {
    }

    @Override
    public String getContentId() {
        return contentId;
    }

    @Override
    public void setContentId(String contentId) {
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

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
}
