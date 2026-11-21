package internal.org.springframework.content.rest.support;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@EntityListeners(AuditingEntityListener.class)
public class AuditedNestedContentEntity {
    private @Id
    @GeneratedValue Long id;

    private @Version Long version;
    private @CreatedDate Date createdDate;
    private @LastModifiedDate Date modifiedDate;

    private @Embedded AuditedNestedContentChild child = new AuditedNestedContentChild();
    private @RestResource
    @ElementCollection(fetch = FetchType.EAGER) List<AuditedNestedContentChild> children = new ArrayList<>();

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

    public AuditedNestedContentChild getChild() {
        return child;
    }

    public void setChild(AuditedNestedContentChild child) {
        this.child = child;
    }

    public List<AuditedNestedContentChild> getChildren() {
        return children;
    }

    public void setChildren(List<AuditedNestedContentChild> children) {
        this.children = children;
    }
}
