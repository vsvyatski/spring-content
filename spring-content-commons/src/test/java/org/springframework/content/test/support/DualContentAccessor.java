package org.springframework.content.test.support;

/**
 * Dual content-property accessors (default content plus rendition).
 *
 * @author Vladimir Svyatski
 */
public interface DualContentAccessor extends ContentAccessor {

    String getRenditionId();

    @SuppressWarnings("unused") // set via BeanUtils / store metadata, not direct calls
    void setRenditionId(String renditionId);

    Long getRenditionLen();

    @SuppressWarnings("unused") // set via BeanUtils / store metadata, not direct calls
    void setRenditionLen(Long renditionLen);
}
