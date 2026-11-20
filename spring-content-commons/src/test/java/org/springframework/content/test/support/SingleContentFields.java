package org.springframework.content.test.support;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;

/**
 * Single {@code @ContentId} / {@code @ContentLength} field pair for store tests.
 *
 * @author Vladimir Svyatski
 */
public class SingleContentFields implements ContentAccessor {

    @ContentId
    private String contentId;

    @ContentLength
    private Long contentLen;

    public SingleContentFields() {
        this(null);
    }

    public SingleContentFields(String contentId) {
        this.contentId = contentId;
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
}