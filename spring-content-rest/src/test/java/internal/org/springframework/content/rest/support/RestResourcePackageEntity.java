package internal.org.springframework.content.rest.support;

import jakarta.persistence.*;
import org.springframework.content.rest.RestResource;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Date;

@Entity
@EntityListeners(AuditingEntityListener.class)
public class RestResourcePackageEntity {

    @Id
    @GeneratedValue
    private Long id;

    @Version
    private Long version;

    @LastModifiedDate
    private Date modifiedDate;

    @Embedded
    @RestResource(linkRel = "package", path = "package")
    private final DualContentEmbeddedChild contentPackage = new DualContentEmbeddedChild();

    public RestResourcePackageEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    @SuppressWarnings("unused")
    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public DualContentEmbeddedChild getContentPackage() {
        return contentPackage;
    }
}