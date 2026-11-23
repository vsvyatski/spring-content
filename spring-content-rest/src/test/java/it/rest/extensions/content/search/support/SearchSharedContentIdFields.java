package it.rest.extensions.content.search.support;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.springframework.content.commons.annotations.ContentId;

import java.util.UUID;

@MappedSuperclass
public abstract class SearchSharedContentIdFields {

    @Id
    @ContentId
    private String id = UUID.randomUUID().toString();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContentId() {
        return id;
    }

    public void setContentId(String id) {
        this.id = id;
    }
}