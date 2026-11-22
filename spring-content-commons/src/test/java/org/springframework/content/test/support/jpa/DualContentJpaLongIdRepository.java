package org.springframework.content.test.support.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Vladimir Svyatski
 */
public interface DualContentJpaLongIdRepository extends JpaRepository<DualContentJpaLongIdEntity, Long> {
}
