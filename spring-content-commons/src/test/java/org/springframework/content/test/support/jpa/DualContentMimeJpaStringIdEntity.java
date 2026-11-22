package org.springframework.content.test.support.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.springframework.content.test.support.DualContentMimeFields;

import java.util.UUID;

/**
 * JPA entity with string id, dual content, and MIME metadata (associative JPA store ITs).
 *
 * @author Vladimir Svyatski
 */
@Entity
public class DualContentMimeJpaStringIdEntity extends DualContentMimeFields {

    @Id
    @org.springframework.data.annotation.Id
    private String id = UUID.randomUUID().toString();

    public DualContentMimeJpaStringIdEntity() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
