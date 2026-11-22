package org.springframework.content.test.support.mongo;

import jakarta.persistence.Id;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.test.support.ContentAccessor;

/**
 * Mongo document whose Jakarta {@code @Id} is also the {@code @ContentId}.
 *
 * @author Vladimir Svyatski
 */
public class SharedIdJakartaMongoDocument implements ContentAccessor<String> {

    @Id
    @ContentId
    private String contentId;

    @ContentLength
    private Long contentLen;

    public SharedIdJakartaMongoDocument() {
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
