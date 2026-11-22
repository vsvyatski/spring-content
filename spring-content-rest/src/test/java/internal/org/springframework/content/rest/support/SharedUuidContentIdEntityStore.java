package internal.org.springframework.content.rest.support;

import org.springframework.content.fs.store.FileSystemContentStore;

import java.util.UUID;

@SuppressWarnings("unused") // classpath / store component scan
public interface SharedUuidContentIdEntityStore extends FileSystemContentStore<SharedUuidContentIdEntity, UUID> {
}
