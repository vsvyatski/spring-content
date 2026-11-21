package org.springframework.content.test.support;

import org.jspecify.annotations.Nullable;

import java.io.Serializable;

/**
 * Content metadata accessors for store integration-test fixtures.
 *
 * @author Vladimir Svyatski
 */
public interface ContentAccessor<CID extends Serializable> {

    @Nullable CID getContentId();

    void setContentId(@Nullable CID contentId);

    @Nullable Long getContentLen();

    void setContentLen(@Nullable Long contentLen);
}
