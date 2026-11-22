package internal.org.springframework.content.rest.support;

import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.fs.store.FileSystemContentStore;
import org.springframework.content.rest.StoreRestResource;
import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin(origins = "http://www.someurl.com")
@StoreRestResource(path = "uuidSingleContentEntitiesContent")
public interface UuidSingleContentEntityStore extends FileSystemContentStore<UuidSingleContentEntity, Long>,
    Renderable<UuidSingleContentEntity> {
}
