package internal.org.springframework.content.rest.support;

import jakarta.persistence.*;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.test.support.UuidDualContentFields;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Date;

@Entity
@EntityListeners(AuditingEntityListener.class)
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

    @CreatedDate
    private Date createdDate;

    @LastModifiedDate
    private Date modifiedDate;

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

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }
}
