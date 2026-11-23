package org.springframework.content.test.support;

import org.jspecify.annotations.Nullable;

/**
 * Dual content-property accessors (default content plus rendition).
 *
 * @author Vladimir Svyatski
 */
public interface DualContentAccessor extends ContentAccessor<String> {

    @Nullable String getRenditionId();

    void setRenditionId(@Nullable String renditionId);

    @Nullable Long getRenditionLen();

    // set via BeanUtils / store metadata
    @SuppressWarnings("unused")
    void setRenditionLen(@Nullable Long renditionLen);
}
