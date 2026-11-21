package internal.org.springframework.content.rest.scoped.revision;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.hibernate.envers.Audited;
import org.jspecify.annotations.Nullable;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.test.support.ContentAccessor;

/**
 * Envers revision REST tests. Field layout matches
 * {@link org.springframework.content.test.support.jpa.SingleContentJpaLongIdEntity} + MIME; implements
 * {@link ContentAccessor} without extending {@link org.springframework.content.test.support.SingleContentFields} so
 * Envers and revision content REST behave correctly.
 *
 * @author Vladimir Svyatski
 */
@Entity
@Audited
public class RevisionPropertyEntity implements ContentAccessor<String> {

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

    @Override
    public @Nullable String getContentId() {
        return contentId;
    }

    @Override
    public void setContentId(@Nullable String contentId) {
        this.contentId = contentId;
    }

    @Override
    public @Nullable Long getContentLen() {
        return contentLen;
    }

    @Override
    public void setContentLen(@Nullable Long contentLen) {
        this.contentLen = contentLen;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
}