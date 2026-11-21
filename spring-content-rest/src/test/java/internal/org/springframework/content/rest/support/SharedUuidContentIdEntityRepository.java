package internal.org.springframework.content.rest.support;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.UUID;

@SuppressWarnings("unused") // classpath / store component scan
@RepositoryRestResource(path = "testEntity6s")
public interface SharedUuidContentIdEntityRepository extends CrudRepository<SharedUuidContentIdEntity, UUID> {
}
