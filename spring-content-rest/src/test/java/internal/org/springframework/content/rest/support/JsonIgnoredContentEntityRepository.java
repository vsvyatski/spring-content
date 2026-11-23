package internal.org.springframework.content.rest.support;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "jsonIgnoredContentEntities")
public interface JsonIgnoredContentEntityRepository extends CrudRepository<JsonIgnoredContentEntity, Long> {
}
