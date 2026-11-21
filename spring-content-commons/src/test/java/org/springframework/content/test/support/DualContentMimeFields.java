package org.springframework.content.test.support;

import org.springframework.content.commons.annotations.MimeType;

/**
 * {@link DualContentFields} plus MIME on the primary content property; includes JPA embeddable {@code contentLength} aliases.
 *
 * @author Vladimir Svyatski
 */
public class DualContentMimeFields extends DualContentFields {

    @MimeType
    private String contentMimeType = "text/plain";

    public DualContentMimeFields() {
    }

    public String getContentMimeType() {
        return contentMimeType;
    }

    public void setContentMimeType(String contentMimeType) {
        this.contentMimeType = contentMimeType;
    }

    /** Alias matching legacy JPA embeddable bean names in content store ITs. */
    public Long getContentLength() {
        return getContentLen();
    }

    public void setContentLength(Long contentLength) {
        setContentLen(contentLength);
    }
}