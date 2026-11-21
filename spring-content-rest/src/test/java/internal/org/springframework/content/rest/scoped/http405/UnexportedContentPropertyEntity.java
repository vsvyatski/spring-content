package internal.org.springframework.content.rest.scoped.http405;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.jspecify.annotations.Nullable;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.rest.RestResource;
import org.springframework.content.test.support.ContentAccessor;

import java.util.UUID;

/**
 * HTTP 405 REST tests when the content property is not exported. Same shape as
 * {@link org.springframework.content.test.support.jpa.fixture.SingleContentJpaLongIdUuidMimeEntity} but declares
 * {@link ContentId} locally so {@link RestResource#exported()} applies (see
 * {@link internal.org.springframework.content.rest.mappingcontext.ContentPropertyToExportedContext}).
 *
 * @author Vladimir Svyatski
 */
@Entity
public class UnexportedContentPropertyEntity implements ContentAccessor<UUID> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ContentId
    @RestResource(exported = false)
    private UUID contentId;

    @ContentLength
    private Long contentLen;

    @MimeType
    private String mimeType;

    public UnexportedContentPropertyEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public @Nullable UUID getContentId() {
        return contentId;
    }

    @Override
    public void setContentId(@Nullable UUID contentId) {
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