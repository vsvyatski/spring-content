package org.springframework.content.test.support;

import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Dual UUID content properties (primary plus rendition), parallel to {@link DualContentAccessor} for {@link String} ids.
 *
 * @author Vladimir Svyatski
 */
public interface UuidDualContentAccessor extends ContentAccessor<UUID> {

    @Nullable
    UUID getRenditionId();

    @SuppressWarnings("unused")
    void setRenditionId(@Nullable UUID renditionId);

    @Nullable
    Long getRenditionLen();

    @SuppressWarnings("unused")
    void setRenditionLen(@Nullable Long renditionLen);
}