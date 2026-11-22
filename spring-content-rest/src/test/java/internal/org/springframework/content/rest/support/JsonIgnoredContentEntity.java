package internal.org.springframework.content.rest.support;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.test.support.UuidSingleContentFields;

import java.util.UUID;

@Entity
public class JsonIgnoredContentEntity extends UuidSingleContentFields {

    public @Id
    @GeneratedValue
    Long id;

    private String name;

    @MimeType
    private String contentMimeType;

    @JsonIgnore
    private String hidden;

    public JsonIgnoredContentEntity() {
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

    public String getHidden() {
        return hidden;
    }

    public void setHidden(String hidden) {
        this.hidden = hidden;
    }

    public String getContentMimeType() {
        return contentMimeType;
    }

    public void setContentMimeType(String contentMimeType) {
        this.contentMimeType = contentMimeType;
    }
}
