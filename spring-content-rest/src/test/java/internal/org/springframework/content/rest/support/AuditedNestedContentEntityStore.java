package internal.org.springframework.content.rest.support;

import org.springframework.content.fs.store.FileSystemContentStore;
import org.springframework.content.rest.StoreRestResource;

@StoreRestResource(path = "files")
public interface AuditedNestedContentEntityStore extends FileSystemContentStore<AuditedNestedContentEntity, String> {
}
