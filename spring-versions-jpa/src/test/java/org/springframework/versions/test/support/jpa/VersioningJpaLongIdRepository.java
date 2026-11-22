package org.springframework.versions.test.support.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.versions.LockingAndVersioningRepository;

/**
 * @author Vladimir Svyatski
 */
public interface VersioningJpaLongIdRepository
    extends JpaRepository<VersioningJpaLongIdEntity, Long>, LockingAndVersioningRepository<VersioningJpaLongIdEntity> {
}