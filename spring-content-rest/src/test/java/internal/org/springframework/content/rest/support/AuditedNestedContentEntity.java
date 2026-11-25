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
    @AttributeOverrides({
        @AttributeOverride(name = "contentId", column = @Column(name = "child_content_id")),
        @AttributeOverride(name = "contentLen", column = @Column(name = "child_content_len")),
        @AttributeOverride(name = "mimeType", column = @Column(name = "child_mime_type"))
    })
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

    @SuppressWarnings("unused")
    public void setChild(AuditedNestedContentChild child) {
        this.child = child;
    }
}
