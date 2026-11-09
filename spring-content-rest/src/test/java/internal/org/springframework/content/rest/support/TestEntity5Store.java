package internal.org.springframework.content.rest.support;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.fs.store.FileSystemContentStore;
import org.springframework.content.rest.RestResource;
import org.springframework.content.rest.StoreRestResource;

import java.io.InputStream;
import java.util.UUID;

@StoreRestResource
public interface TestEntity5Store extends FileSystemContentStore<TestEntity5, UUID> {

    @RestResource(exported=false, paths={"rendition"})
    @Override
    InputStream getContent(TestEntity5 entity, PropertyPath propertyPath);
}
