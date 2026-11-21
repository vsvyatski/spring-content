package org.springframework.content.test.support.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.springframework.content.test.support.SingleContentFields;

/**
 * JPA entity with a generated long primary key and a single content property (boot auto-config tests).
 *
 * @author Vladimir Svyatski
 */
@Entity
public class SingleContentJpaLongIdEntity extends SingleContentFields {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    public SingleContentJpaLongIdEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
