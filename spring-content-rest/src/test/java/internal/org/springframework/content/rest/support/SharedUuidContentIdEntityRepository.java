package internal.org.springframework.content.rest.support;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.UUID;

@RepositoryRestResource(path = "sharedUuidContentIdEntities")
public interface SharedUuidContentIdEntityRepository extends CrudRepository<SharedUuidContentIdEntity, UUID> {
}
