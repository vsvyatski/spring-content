package org.springframework.content.test.support.fulltext;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.test.support.UuidSingleContentFields;

/**
 * JPA entity for Elasticsearch/Solr full-text indexing ITs (UUID content id, title, author metadata).
 *
 * @author Vladimir Svyatski
 */
@Entity
public class FulltextJpaUuidDocumentEntity extends UuidSingleContentFields {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @MimeType
    private String mimeType;

    private String title;

    private String author;

    public FulltextJpaUuidDocumentEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
}