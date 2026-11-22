package internal.org.springframework.content.rest.support;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "oneToOneLinkedContentEntities")
public interface OneToOneLinkedContentEntityRepository extends JpaRepository<OneToOneLinkedContentEntity, Long> {
}
