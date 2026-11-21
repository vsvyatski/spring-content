package org.springframework.content.test.support;

import java.io.Serializable;

/**
 * Content metadata accessors for store integration-test fixtures.
 *
 * @author Vladimir Svyatski
 */
public interface ContentAccessor<CID extends Serializable> {

    CID getContentId();

    void setContentId(CID contentId);

    Long getContentLen();

    void setContentLen(Long contentLen);
}
