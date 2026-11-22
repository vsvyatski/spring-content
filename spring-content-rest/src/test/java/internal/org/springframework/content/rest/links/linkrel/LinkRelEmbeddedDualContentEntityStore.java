package internal.org.springframework.content.rest.links.linkrel;

import internal.org.springframework.content.rest.support.EmbeddedDualContentEntity;
import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.fs.store.FileSystemContentStore;
import org.springframework.content.rest.StoreRestResource;

import java.util.UUID;

@StoreRestResource(path = "embeddedDualContentEntities", linkRel = "foo")
public interface LinkRelEmbeddedDualContentEntityStore extends FileSystemContentStore<EmbeddedDualContentEntity, UUID>,
    Renderable<EmbeddedDualContentEntity> {
}
