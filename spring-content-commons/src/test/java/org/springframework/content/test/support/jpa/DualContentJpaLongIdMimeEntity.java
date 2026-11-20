package org.springframework.content.test.support.jpa;

import jakarta.persistence.Entity;
import org.springframework.content.commons.annotations.MimeType;

/**
 * S3/GCS/Azure IT entity: dual content plus MIME metadata on each property.
 *
 * @author Vladimir Svyatski
 */
@Entity
public class DualContentJpaLongIdMimeEntity extends DualContentJpaLongIdEntity {

    @MimeType
    private String contentType;
    @MimeType
    private String renditionType;

    public DualContentJpaLongIdMimeEntity() {
    }

    public DualContentJpaLongIdMimeEntity(String contentId) {
        super(contentId);
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getRenditionType() {
        return renditionType;
    }

    public void setRenditionType(String renditionType) {
        this.renditionType = renditionType;
    }
}
