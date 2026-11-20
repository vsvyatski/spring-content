package org.springframework.content.test.support;

/**
 * Content metadata accessors for store integration-test fixtures.
 *
 * @author Vladimir Svyatski
 */
public interface ContentAccessor {

    String getContentId();

    void setContentId(String contentId);

    Long getContentLen();

    void setContentLen(Long contentLen);
}
