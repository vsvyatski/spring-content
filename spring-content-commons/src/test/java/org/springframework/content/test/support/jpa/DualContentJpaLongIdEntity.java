package org.springframework.content.test.support.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import org.springframework.content.test.support.DualContentFields;

/**
 * JPA entity with a generated long primary key and dual content properties (S3/GCS/Azure IT shape).
 *
 * @author Vladimir Svyatski
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class DualContentJpaLongIdEntity extends DualContentFields {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    public DualContentJpaLongIdEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}