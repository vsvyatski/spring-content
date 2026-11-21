package internal.org.springframework.content.rest.scoped.revision;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.hibernate.envers.Audited;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;

/**
 * Envers revision REST tests. Field layout matches
 * {@link org.springframework.content.test.support.jpa.SingleContentJpaLongIdEntity} + MIME; kept standalone because
 * Envers cannot audit through an unaudited {@code @Entity} superclass and revision content REST returns 404 when
 * extending {@link org.springframework.content.test.support.SingleContentFields} without a JPA embeddable split.
 *
 * @author Vladimir Svyatski
 */
@Entity
@Audited
public class RevisionPropertyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ContentId
    private String contentId;

    @ContentLength
    private Long contentLen;

    @MimeType
    private String mimeType;

    public RevisionPropertyEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    public Long getContentLen() {
        return contentLen;
    }

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