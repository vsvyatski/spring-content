package org.springframework.content.test.support;

/**
 * Dual content-property accessors (default content plus rendition).
 *
 * @author Vladimir Svyatski
 */
public interface DualContentAccessor extends ContentAccessor<String> {

    String getRenditionId();

    // set via BeanUtils / store metadata, not direct calls
    @SuppressWarnings("unused")
    void setRenditionId(String renditionId);

    Long getRenditionLen();

    // set via BeanUtils / store metadata, not direct calls
    @SuppressWarnings("unused")
    void setRenditionLen(Long renditionLen);
}
