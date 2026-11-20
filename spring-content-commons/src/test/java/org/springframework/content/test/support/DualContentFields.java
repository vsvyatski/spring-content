package org.springframework.content.test.support;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;

/**
 * Two content properties (default and rendition) for {@link org.springframework.content.commons.property.PropertyPath}
 * store tests.
 *
 * @author Vladimir Svyatski
 */
public class DualContentFields implements DualContentAccessor {

    @ContentId
    private String contentId;

    @ContentLength
    private Long contentLen;

    @ContentId
    private String renditionId;

    @ContentLength
    private Long renditionLen;

    public DualContentFields() {
        this.contentId = null;
    }

    public DualContentFields(String contentId) {
        this.contentId = contentId;
    }

    @Override
    public String getContentId() {
        return contentId;
    }

    @Override
    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    @Override
    public Long getContentLen() {
        return contentLen;
    }

    @Override
    public void setContentLen(Long contentLen) {
        this.contentLen = contentLen;
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
