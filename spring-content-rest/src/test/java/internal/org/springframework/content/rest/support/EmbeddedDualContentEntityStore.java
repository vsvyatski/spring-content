package internal.org.springframework.content.rest.support;

import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.fs.store.FileSystemContentStore;
import org.springframework.content.rest.StoreRestResource;

import java.util.UUID;

@StoreRestResource(path = "testEntity10s")
public interface EmbeddedDualContentEntityStore extends FileSystemContentStore<EmbeddedDualContentEntity, UUID>, Renderable<EmbeddedDualContentEntity> {
}
