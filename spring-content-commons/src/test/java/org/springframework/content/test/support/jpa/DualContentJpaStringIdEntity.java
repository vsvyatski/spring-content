package org.springframework.content.test.support.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.content.test.support.DualContentFields;

import java.util.UUID;

/**
 * JPA entity with a string primary key and dual content properties (file-system IT shape).
 *
 * @author Vladimir Svyatski
 */
@Entity
@Table(name = "tEntity")
public class DualContentJpaStringIdEntity extends DualContentFields {

    @Id
    private String id = UUID.randomUUID().toString();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
