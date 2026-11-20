package org.springframework.content.test.support;

/**
 * {@link ContentFields} with {@link String} {@linkplain org.springframework.content.commons.annotations.ContentId}.
 *
 * @author Vladimir Svyatski
 */
public class SingleContentFields extends ContentFields<String> implements ContentAccessor {

    public SingleContentFields() {
    }

    public SingleContentFields(String contentId) {
        super(contentId);
    }
}
