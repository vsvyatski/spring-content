package org.springframework.content.test.support.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Vladimir Svyatski
 */
public interface DualContentJpaStringIdRepository extends JpaRepository<DualContentJpaStringIdEntity, String> {
}