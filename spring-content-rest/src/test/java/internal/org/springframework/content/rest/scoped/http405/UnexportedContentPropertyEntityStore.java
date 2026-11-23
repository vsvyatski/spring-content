package internal.org.springframework.content.rest.scoped.http405;

import org.springframework.content.fs.store.FileSystemContentStore;

import java.util.UUID;

/**
 * @author Vladimir Svyatski
 */
public interface UnexportedContentPropertyEntityStore
    extends FileSystemContentStore<UnexportedContentPropertyEntity, UUID> {
}
