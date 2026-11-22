package org.springframework.versions.test.support.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.versions.LockingAndVersioningRepository;

/**
 * @author Vladimir Svyatski
 */
public interface VersioningJpaSiblingRepository extends JpaRepository<VersioningJpaSiblingEntity, Long>,
    LockingAndVersioningRepository<VersioningJpaSiblingEntity> {
}
