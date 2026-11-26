package internal.org.springframework.content.rest.support;

import org.jspecify.annotations.NullMarked;
import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.fs.store.FileSystemContentStore;
import org.springframework.content.rest.RestResource;
import org.springframework.content.rest.StoreRestResource;

import java.io.InputStream;

@StoreRestResource(path = "jsonMappedContentEntities")
@NullMarked
public interface JsonMappedContentEntityStore extends FileSystemContentStore<JsonMappedContentEntity, Long>,
    Renderable<JsonMappedContentEntity> {

    @RestResource(exported = false)
    @Override
    JsonMappedContentEntity setContent(JsonMappedContentEntity property, InputStream content);
}
