package internal.org.springframework.content.rest.support;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@SuppressWarnings("unused") // classpath / store component scan
@RepositoryRestResource(path = "restResourcePackageEntities")
public interface RestResourcePackageEntityRepository extends CrudRepository<RestResourcePackageEntity, Long> {
}
