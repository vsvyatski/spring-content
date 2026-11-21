package org.springframework.content.test.support.jpa;

import org.springframework.content.commons.store.ContentStore;

/**
 * @author Vladimir Svyatski
 */
public interface EmbeddedContentJpaStringIdStore extends ContentStore<EmbeddedContentJpaStringIdEntity, String> {
}