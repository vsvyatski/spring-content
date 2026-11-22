package internal.org.springframework.content.rest.scoped.revision;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author Vladimir Svyatski
 */
@SuppressWarnings("unused") // classpath / store component scan
@RepositoryRestResource(path = "revisionPropertyEntities")
public interface RevisionPropertyEntityRepository extends JpaRepository<RevisionPropertyEntity, Long>,
    RevisionRepository<RevisionPropertyEntity, Long, Integer> {
}
