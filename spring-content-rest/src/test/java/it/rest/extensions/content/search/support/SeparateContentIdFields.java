package it.rest.extensions.content.search.support;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.springframework.content.commons.annotations.ContentId;

import java.util.UUID;

@MappedSuperclass
public abstract class SeparateContentIdFields {

    @Id
    private String id = UUID.randomUUID().toString();

    @ContentId
    private String contentId = UUID.randomUUID().toString();

    public SeparateContentIdFields() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }
}