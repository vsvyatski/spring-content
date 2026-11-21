package org.springframework.content.test.support.fulltext;

import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.commons.search.Searchable;
import org.springframework.content.commons.store.ContentStore;

import java.util.UUID;

/**
 * @author Vladimir Svyatski
 */
public interface FulltextJpaUuidDocumentStore
    extends ContentStore<FulltextJpaUuidDocumentEntity, UUID>, Searchable<UUID>, Renderable<FulltextJpaUuidDocumentEntity> {
}
