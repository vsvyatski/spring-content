package org.springframework.versions.test.support.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * Second {@code LockingAndVersioningRepository} entity for multi-repository versioning ITs.
 *
 * @author Vladimir Svyatski
 */
@Entity
public class VersioningJpaSiblingEntity {

    @Id
    @GeneratedValue
    private Long id;

    public VersioningJpaSiblingEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
