package internal.org.springframework.content.rest.support;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "jsonMappedContentEntities")
public interface JsonMappedContentEntityRepository extends JpaRepository<JsonMappedContentEntity, Long> {
}
