package org.springframework.content.test.support.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.test.support.ContentAccessor;

/**
 * JPA entity whose Spring Data and Jakarta {@code @Id} are also the {@code @ContentId}.
 *
 * @author Vladimir Svyatski
 */
@Entity
public class SharedIdSpringJpaEntity implements ContentAccessor<String> {

    @Id
    @org.springframework.data.annotation.Id
    @ContentId
    private String contentId;

    @ContentLength
    private Long contentLen;

    public SharedIdSpringJpaEntity() {
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
