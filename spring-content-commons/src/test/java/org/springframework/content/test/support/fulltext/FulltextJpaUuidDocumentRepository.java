package org.springframework.content.test.support.fulltext;

import org.springframework.data.repository.CrudRepository;

/**
 * @author Vladimir Svyatski
 */
public interface FulltextJpaUuidDocumentRepository extends CrudRepository<FulltextJpaUuidDocumentEntity, Long> {
}
