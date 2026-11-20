package org.springframework.content.test.support.mongo;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.test.support.ContentAccessor;
import org.springframework.data.annotation.Id;

/**
 * Mongo document whose Spring Data {@code @Id} is also the {@code @ContentId}.
 *
 * @author Vladimir Svyatski
 */
public class SharedIdSpringMongoDocument implements ContentAccessor {

    @Id
    @ContentId
    private String contentId;

    @ContentLength
    private Long contentLen;

    public SharedIdSpringMongoDocument() {
        this.contentId = null;
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