package internal.org.springframework.content.rest.support;

import jakarta.persistence.*;

@Entity
public class AuditedNestedContentEntity {

    @Id
    @GeneratedValue
    private Long id;

    @Version
    private Long version;

    @Embedded
    private AuditedNestedContentChild child = new AuditedNestedContentChild();

    public AuditedNestedContentEntity() {
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

    public AuditedNestedContentChild getChild() {
        return child;
    }

    public void setChild(AuditedNestedContentChild child) {
        this.child = child;
    }
}
