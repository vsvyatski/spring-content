package org.springframework.content.test.support.jpa.fixture;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.test.support.UuidSingleContentFields;

/**
 * Long surrogate key, {@link java.util.UUID} content id, and MIME metadata.
 *
 * @author Vladimir Svyatski
 */
@Entity
public class SingleContentJpaLongIdUuidMimeEntity extends UuidSingleContentFields {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @MimeType
    private String mimeType;

    public SingleContentJpaLongIdUuidMimeEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
}
