package org.springframework.content.test.support.jpa.fixture;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.test.support.UuidSingleContentFields;

/**
 * Long surrogate key, {@link java.util.UUID} content id, optional {@code name}, and MIME metadata.
 *
 * @author Vladimir Svyatski
 */
@Entity
public class SingleContentJpaLongIdUuidNamedEntity extends UuidSingleContentFields {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    @MimeType
    private String mimeType;

    public SingleContentJpaLongIdUuidNamedEntity() {
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

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
}
