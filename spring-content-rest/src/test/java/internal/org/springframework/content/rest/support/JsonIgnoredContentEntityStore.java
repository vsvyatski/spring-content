package internal.org.springframework.content.rest.support;

import org.springframework.content.fs.store.FileSystemContentStore;

import java.util.UUID;

public interface JsonIgnoredContentEntityStore extends FileSystemContentStore<JsonIgnoredContentEntity, UUID> {
}

