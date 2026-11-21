package internal.org.springframework.content.rest.scoped.http405;

import org.springframework.content.fs.store.FileSystemContentStore;

import java.util.UUID;

/**
 * @author Vladimir Svyatski
 */
@SuppressWarnings("unused") // classpath / store component scan
public interface UnexportedContentPropertyEntityStore
    extends FileSystemContentStore<UnexportedContentPropertyEntity, UUID> {
}