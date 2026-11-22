package internal.org.springframework.content.rest.support;

import jakarta.persistence.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Date;

@Entity
@EntityListeners(AuditingEntityListener.class)
public class EmbeddedChildContentEntity {

    @Embedded
    private final EmbeddedContentChild child = new EmbeddedContentChild();
    @Id
    @GeneratedValue
    private Long id;
    private String name;
    @Version
    private Long version;
    @LastModifiedDate
    private Date modifiedDate;

    public EmbeddedChildContentEntity() {
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

    public EmbeddedContentChild getChild() {
        return child;
    }
}
