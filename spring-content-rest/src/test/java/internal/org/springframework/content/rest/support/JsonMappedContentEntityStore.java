package internal.org.springframework.content.rest.support;

import org.jspecify.annotations.NonNull;
import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.fs.store.FileSystemContentStore;
import org.springframework.content.rest.RestResource;
import org.springframework.content.rest.StoreRestResource;

import java.io.InputStream;

@StoreRestResource(path = "jsonMappedContentEntities")
public interface JsonMappedContentEntityStore extends FileSystemContentStore<JsonMappedContentEntity, Long>,
    Renderable<JsonMappedContentEntity> {

    @RestResource(exported = false)
    @Override
    JsonMappedContentEntity setContent(@NonNull JsonMappedContentEntity property, InputStream content);
}
