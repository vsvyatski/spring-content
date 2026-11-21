package internal.org.springframework.content.rest.support;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Date;

@Entity
@EntityListeners(AuditingEntityListener.class)
public class EmbeddedDualContentEntity {

    @Id
    @GeneratedValue
    private Long id;
    private String name;
    @JsonIgnore
    private String hidden;
    private @Version Long version;
    private @CreatedDate Date createdDate;
    private @LastModifiedDate Date modifiedDate;
    private @Embedded DualContentEmbeddedChild child = new DualContentEmbeddedChild();

    public EmbeddedDualContentEntity() {
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

    public String getHidden() {
        return hidden;
    }

    public void setHidden(String hidden) {
        this.hidden = hidden;
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

    public DualContentEmbeddedChild getChild() {
        return child;
    }

    public void setChild(DualContentEmbeddedChild child) {
        this.child = child;
    }
}
