package org.springframework.content.test.support;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;

/**
 * Two content properties (default and rendition) for {@link org.springframework.content.commons.property.PropertyPath}
 * store tests.
 *
 * @author Vladimir Svyatski
 */
public class DualContentFields extends SingleContentFields implements DualContentAccessor {

    @ContentId
    private String renditionId;

    @ContentLength
    private Long renditionLen;

    public DualContentFields() {
    }

    @Override
    public String getRenditionId() {
        return renditionId;
    }

    @Override
    public void setRenditionId(String renditionId) {
        this.renditionId = renditionId;
    }

    @Override
    public Long getRenditionLen() {
        return renditionLen;
    }

    @Override
    public void setRenditionLen(Long renditionLen) {
        this.renditionLen = renditionLen;
    }
}
