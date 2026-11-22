package org.springframework.content.test.support.jpa;

import org.springframework.data.repository.CrudRepository;

/**
 * @author Vladimir Svyatski
 */
public interface NestedDualContentJpaStringIdRepository
    extends CrudRepository<NestedDualContentJpaStringIdEntity, String> {
}
