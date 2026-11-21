package internal.org.springframework.content.rest.support;

import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.fs.store.FileSystemContentStore;
import org.springframework.content.rest.RestResource;

import java.io.InputStream;

@SuppressWarnings("unused") // classpath / store component scan
public interface JsonMappedContentEntityStore extends FileSystemContentStore<JsonMappedContentEntity, Long>, Renderable<JsonMappedContentEntity> {

    @RestResource(exported = false)
    @Override
    JsonMappedContentEntity setContent(JsonMappedContentEntity property, InputStream content);
}
