package internal.org.springframework.content.rest.links.linkrel;

import internal.org.springframework.content.rest.support.AuditedNestedContentEntity;
import org.springframework.content.fs.store.FileSystemContentStore;
import org.springframework.content.rest.StoreRestResource;

import java.util.UUID;

@StoreRestResource(path = "files", linkRel = "foo")
public interface LinkRelAuditedNestedContentEntityStore extends FileSystemContentStore<AuditedNestedContentEntity, UUID> {
}
