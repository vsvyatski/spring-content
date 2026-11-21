package org.springframework.content.test.support;

import org.jspecify.annotations.Nullable;

/**
 * Dual content-property accessors (default content plus rendition).
 *
 * @author Vladimir Svyatski
 */
public interface DualContentAccessor extends ContentAccessor<String> {

    @Nullable String getRenditionId();

    // set via BeanUtils / store metadata, not direct calls
    @SuppressWarnings("unused")
    void setRenditionId(@Nullable String renditionId);

    @Nullable Long getRenditionLen();

    // set via BeanUtils / store metadata, not direct calls
    @SuppressWarnings("unused")
    void setRenditionLen(@Nullable Long renditionLen);
}
