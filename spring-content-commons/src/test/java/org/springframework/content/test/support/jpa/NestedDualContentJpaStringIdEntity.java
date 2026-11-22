package org.springframework.content.test.support.jpa;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.util.UUID;

/**
 * JPA root entity with embedded dual content for {@code claimForm/*} property-path store ITs.
 *
 * @author Vladimir Svyatski
 */
@Entity
public class NestedDualContentJpaStringIdEntity {

    @Id
    @org.springframework.data.annotation.Id
    private String claimId = UUID.randomUUID().toString();

    private String lastName;
    private String firstName;

    @Embedded
    private NestedDualContentJpaEmbeddable claimForm = new NestedDualContentJpaEmbeddable();

    public NestedDualContentJpaStringIdEntity() {
    }

    @SuppressWarnings("unused") // JPA id + CrudRepository
    public String getClaimId() {
        return claimId;
    }

    @SuppressWarnings("unused")
    public void setClaimId(String claimId) {
        this.claimId = claimId;
    }

    @SuppressWarnings("unused") // set in ContentStoreIT; not asserted
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @SuppressWarnings("unused")
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public NestedDualContentJpaEmbeddable getClaimForm() {
        return claimForm;
    }

    public void setClaimForm(NestedDualContentJpaEmbeddable claimForm) {
        this.claimForm = claimForm;
    }
}
