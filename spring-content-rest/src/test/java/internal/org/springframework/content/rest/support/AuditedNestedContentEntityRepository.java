package internal.org.springframework.content.rest.support;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "files")
public interface AuditedNestedContentEntityRepository extends JpaRepository<AuditedNestedContentEntity, Long> {
}
