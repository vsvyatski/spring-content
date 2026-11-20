package org.springframework.content.test.support;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;

import java.io.Serializable;

/**
 * Single annotated content property; {@code ID} is the {@link ContentId} field type (e.g. {@link String} or
 * {@link java.util.UUID}).
 *
 * @author Vladimir Svyatski
 */
public class ContentFields<ID extends Serializable> {

    @ContentId
    private ID contentId;

    @ContentLength
    private Long contentLen;

    public ContentFields() {
    }

    public ContentFields(ID contentId) {
        this.contentId = contentId;
    }

    public ID getContentId() {
        return contentId;
    }

    public void setContentId(ID contentId) {
        this.contentId = contentId;
    }

    public Long getContentLen() {
        return contentLen;
    }

    public void setContentLen(Long contentLen) {
        this.contentLen = contentLen;
    }
}
