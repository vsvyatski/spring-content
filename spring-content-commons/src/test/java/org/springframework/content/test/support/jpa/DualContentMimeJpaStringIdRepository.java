package org.springframework.content.test.support.jpa;

import org.springframework.data.repository.CrudRepository;

/**
 * @author Vladimir Svyatski
 */
public interface DualContentMimeJpaStringIdRepository
    extends CrudRepository<DualContentMimeJpaStringIdEntity, String> {
}
