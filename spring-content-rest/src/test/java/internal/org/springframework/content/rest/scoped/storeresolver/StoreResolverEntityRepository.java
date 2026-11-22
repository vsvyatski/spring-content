package internal.org.springframework.content.rest.scoped.storeresolver;

import org.springframework.content.test.support.jpa.fixture.SingleContentJpaLongIdMimeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author Vladimir Svyatski
 */
@SuppressWarnings("unused") // classpath / store component scan
@RepositoryRestResource(path = "storeResolverEntities")
public interface StoreResolverEntityRepository extends JpaRepository<SingleContentJpaLongIdMimeEntity, Long> {
}