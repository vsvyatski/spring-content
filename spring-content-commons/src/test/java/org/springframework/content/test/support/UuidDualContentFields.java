package org.springframework.content.test.support;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;

import java.util.UUID;

/**
 * {@link UuidSingleContentFields} plus a second {@link ContentId} / rendition property (UUID-backed dual content).
 *
 * @author Vladimir Svyatski
 */
public class UuidDualContentFields extends UuidSingleContentFields implements UuidDualContentAccessor {

    @ContentId
    private UUID renditionId;

    @ContentLength
    private Long renditionLen;

    public UuidDualContentFields() {
    }

    public UUID getRenditionId() {
        return renditionId;
    }

    public void setRenditionId(UUID renditionId) {
        this.renditionId = renditionId;
    }

    public Long getRenditionLen() {
        return renditionLen;
    }

    public void setRenditionLen(Long renditionLen) {
        this.renditionLen = renditionLen;
    }
}
