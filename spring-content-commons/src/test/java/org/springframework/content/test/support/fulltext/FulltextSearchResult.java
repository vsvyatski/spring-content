package org.springframework.content.test.support.fulltext;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.fulltext.Attribute;
import org.springframework.content.commons.fulltext.Highlight;

import java.util.UUID;

/**
 * Custom return type for full-text search ITs (highlights and synced attributes).
 *
 * @author Vladimir Svyatski
 */
public class FulltextSearchResult {

    @ContentId
    private UUID contentId;

    @Highlight
    private String highlight;

    @Attribute(name = "author")
    private String author;

    public FulltextSearchResult() {
    }

    public UUID getContentId() {
        return contentId;
    }

    public void setContentId(UUID contentId) {
        this.contentId = contentId;
    }

    @SuppressWarnings("unused") // SearchableImpl + AssertJ extracting in Solr/ES ITs
    public String getHighlight() {
        return highlight;
    }

    @SuppressWarnings("unused")
    public void setHighlight(String highlight) {
        this.highlight = highlight;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
}