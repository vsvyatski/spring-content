package org.springframework.content.test.support.jpa;

import jakarta.persistence.Entity;
import java.util.UUID;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.content.test.support.DualContentFields;

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