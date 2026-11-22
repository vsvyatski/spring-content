package org.springframework.content.test.support.jpa;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * JPA entity with {@link EmbeddedJpaContent}, {@link UUID} id, and a column override on embedded {@code contentId}.
 *
 * @author Vladimir Svyatski
 */
@Entity
@Table(name = "property_path_jpa_uuid_entity")
public class PropertyPathJpaUuidEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String number;

    @Embedded
    @AttributeOverride(name = "contentId", column = @Column(name = "content__id"))
    private EmbeddedJpaContent content = new EmbeddedJpaContent();

    public PropertyPathJpaUuidEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public EmbeddedJpaContent getContent() {
        return content;
    }

    public void setContent(EmbeddedJpaContent content) {
        this.content = content;
    }
}
