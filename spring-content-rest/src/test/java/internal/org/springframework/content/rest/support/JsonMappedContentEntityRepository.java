package internal.org.springframework.content.rest.support;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@SuppressWarnings("unused") // classpath / store component scan
@RepositoryRestResource(path = "testEntity3s")
public interface JsonMappedContentEntityRepository extends JpaRepository<JsonMappedContentEntity, Long> {

    JsonMappedContentEntity save(JsonMappedContentEntity entity);
}
