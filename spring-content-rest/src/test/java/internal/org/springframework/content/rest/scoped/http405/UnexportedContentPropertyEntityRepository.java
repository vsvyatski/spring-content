package internal.org.springframework.content.rest.scoped.http405;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author Vladimir Svyatski
 */
@SuppressWarnings("unused") // classpath / store component scan
@RepositoryRestResource(path = "tEntity2s")
public interface UnexportedContentPropertyEntityRepository extends JpaRepository<UnexportedContentPropertyEntity, Long> {
}