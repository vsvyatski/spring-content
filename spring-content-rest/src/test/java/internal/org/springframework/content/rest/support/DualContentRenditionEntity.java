package internal.org.springframework.content.rest.support;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.test.support.UuidDualContentFields;

@Entity
public class DualContentRenditionEntity extends UuidDualContentFields {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    @MimeType
    private String contentMimeType;

    @MimeType
    private String renditionMimeType;

    @Version
    private Long version;

    public DualContentRenditionEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContentMimeType() {
        return contentMimeType;
    }

    @SuppressWarnings("unused")
    public void setContentMimeType(String contentMimeType) {
        this.contentMimeType = contentMimeType;
    }

    public String getRenditionMimeType() {
        return renditionMimeType;
    }

    public void setRenditionMimeType(String renditionMimeType) {
        this.renditionMimeType = renditionMimeType;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
