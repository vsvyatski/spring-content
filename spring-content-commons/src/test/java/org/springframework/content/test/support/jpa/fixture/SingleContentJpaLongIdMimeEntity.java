package org.springframework.content.test.support.jpa.fixture;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.test.support.SingleContentFields;

/**
 * Same shape as {@link org.springframework.content.test.support.jpa.SingleContentJpaLongIdEntity} plus MIME metadata.
 * Extends {@link SingleContentFields} directly so REST persistence units avoid JPA inheritance with the base entity type.
 *
 * @author Vladimir Svyatski
 */
@Entity
public class SingleContentJpaLongIdMimeEntity extends SingleContentFields {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @MimeType
    private String mimeType;

    public SingleContentJpaLongIdMimeEntity() {
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
