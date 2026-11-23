package internal.org.springframework.content.rest.scoped.http405;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author Vladimir Svyatski
 */
@RepositoryRestResource(path = "unexportedContentPropertyEntities")
public interface UnexportedContentPropertyEntityRepository
    extends JpaRepository<UnexportedContentPropertyEntity, Long> {
}
