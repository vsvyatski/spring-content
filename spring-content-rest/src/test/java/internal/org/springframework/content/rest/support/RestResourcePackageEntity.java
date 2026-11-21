package internal.org.springframework.content.rest.support;

import jakarta.persistence.*;
import org.springframework.content.rest.RestResource;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Date;

@Entity
@EntityListeners(AuditingEntityListener.class)
public class RestResourcePackageEntity {

    @Id
    @GeneratedValue
    private Long id;
    private @Version Long version;
    private @CreatedDate Date createdDate;
    private @LastModifiedDate Date modifiedDate;
    @RestResource(linkRel = "package", path = "package")
    private @Embedded DualContentEmbeddedChild _package = new DualContentEmbeddedChild();

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

    public DualContentEmbeddedChild get_package() {
        return _package;
    }

    public void set_package(DualContentEmbeddedChild _package) {
        this._package = _package;
    }
}
