package org.springframework.content.test.support.fulltext;

import org.springframework.content.commons.search.Searchable;
import org.springframework.content.commons.store.ContentStore;

import java.util.UUID;

/**
 * @author Vladimir Svyatski
 */
public interface FulltextJpaUuidDocumentSearchableStore
    extends ContentStore<FulltextJpaUuidDocumentEntity, UUID>, Searchable<FulltextSearchResult> {
}
