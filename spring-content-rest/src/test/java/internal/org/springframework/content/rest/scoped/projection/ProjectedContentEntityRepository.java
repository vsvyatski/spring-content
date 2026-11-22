package internal.org.springframework.content.rest.scoped.projection;

import org.springframework.content.test.support.jpa.fixture.SingleContentJpaLongIdUuidNamedEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author Vladimir Svyatski
 */
@SuppressWarnings("unused") // classpath / store component scan
@RepositoryRestResource(path = "projectedContentEntities")
public interface ProjectedContentEntityRepository extends JpaRepository<SingleContentJpaLongIdUuidNamedEntity, Long> {
}