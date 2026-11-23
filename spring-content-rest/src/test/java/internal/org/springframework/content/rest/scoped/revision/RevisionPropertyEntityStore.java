package internal.org.springframework.content.rest.scoped.revision;

import org.springframework.content.fs.store.FileSystemContentStore;
import org.springframework.content.rest.StoreRestResource;

/**
 * @author Vladimir Svyatski
 */
@StoreRestResource(path = "revisionPropertyEntities")
public interface RevisionPropertyEntityStore extends FileSystemContentStore<RevisionPropertyEntity, String> {
}
