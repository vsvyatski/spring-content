package org.springframework.content.test.support.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * @author Vladimir Svyatski
 */
public interface PropertyPathJpaUuidRepository extends JpaRepository<PropertyPathJpaUuidEntity, UUID> {
}
