package org.springframework.content.test.support.jpa;

import jakarta.persistence.Embeddable;
import org.springframework.content.test.support.DualContentNestedJpaFields;

/**
 * Embedded dual content + MIME for {@code claimForm/*} property-path JPA store ITs.
 *
 * @author Vladimir Svyatski
 */
@Embeddable
public class NestedDualContentJpaEmbeddable extends DualContentNestedJpaFields {

    public NestedDualContentJpaEmbeddable() {
    }

    // Computed getter without a backing field — mapping must ignore spurious properties.
    public boolean getIsActive() {
        return true;
    }
}