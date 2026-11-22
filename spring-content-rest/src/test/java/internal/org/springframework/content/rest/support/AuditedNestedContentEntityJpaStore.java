package internal.org.springframework.content.rest.support;

import org.springframework.content.jpa.store.JpaContentStore;
import org.springframework.content.rest.StoreRestResource;

import java.util.UUID;

@StoreRestResource(path = "files")
public interface AuditedNestedContentEntityJpaStore extends JpaContentStore<AuditedNestedContentEntity, UUID> {
}
