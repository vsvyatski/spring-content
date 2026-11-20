package org.springframework.content.test.support.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.test.support.ContentAccessor;

/**
 * JPA entity whose Jakarta {@code @Id} is also the {@code @ContentId}.
 *
 * @author Vladimir Svyatski
 */
@Entity
@Table(name = "shared_id_entity")
public class SharedIdJakartaJpaEntity implements ContentAccessor {

    @Id
    @ContentId
    private String contentId;

    @ContentLength
    private Long contentLen;

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