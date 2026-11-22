package internal.org.springframework.content.rest.scoped.http405;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author Vladimir Svyatski
 */
@SuppressWarnings("unused") // classpath / store component scan
@RepositoryRestResource(path = "unexportedStoreMethodsEntities")
public interface UnexportedStoreMethodsEntityRepository extends JpaRepository<UnexportedStoreMethodsEntity, Long> {
}