package internal.org.springframework.content.rest.support;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.test.support.ContentAccessor;

import java.util.UUID;

@Entity
public class SharedUuidContentIdEntity implements ContentAccessor<UUID> {

    @Id
    @GeneratedValue
    @ContentId
    private UUID id;

    @ContentLength
    private Long contentLen;

    @MimeType
    private String mimeType;

    public SharedUuidContentIdEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    public UUID getContentId() {
        return id;
    }

    @Override
    public void setContentId(UUID contentId) {
        this.id = contentId;
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
