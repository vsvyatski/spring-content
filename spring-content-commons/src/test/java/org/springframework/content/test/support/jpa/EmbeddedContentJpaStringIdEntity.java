package org.springframework.content.test.support.jpa;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * JPA entity with a null-default {@link EmbeddedJpaContent} property (embedded content store ITs).
 *
 * @author Vladimir Svyatski
 */
@Entity
@Table(name = "entity_with_embedded")
public class EmbeddedContentJpaStringIdEntity {

    @Id
    private String id = UUID.randomUUID().toString();

    @Embedded
    private EmbeddedJpaContent content;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public EmbeddedJpaContent getContent() {
        return content;
    }

    public void setContent(EmbeddedJpaContent content) {
        this.content = content;
    }
}