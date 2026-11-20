package org.springframework.content.test.support;

/**
 * Dual content-property accessors (default content plus rendition).
 *
 * @author Vladimir Svyatski
 */
public interface DualContentAccessor extends ContentAccessor {

    String getRenditionId();

    void setRenditionId(String renditionId);

    Long getRenditionLen();

    void setRenditionLen(Long renditionLen);
}