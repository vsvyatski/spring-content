package internal.org.springframework.content.rest.scoped.http405;

import jakarta.persistence.Entity;
import org.springframework.content.test.support.jpa.fixture.SingleContentJpaLongIdUuidMimeEntity;

/**
 * HTTP 405 REST tests when store content methods are not exported.
 *
 * @author Vladimir Svyatski
 */
@Entity
public class UnexportedStoreMethodsEntity extends SingleContentJpaLongIdUuidMimeEntity {

    public UnexportedStoreMethodsEntity() {
    }
}
