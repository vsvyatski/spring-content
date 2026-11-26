package internal.org.springframework.content.rest.links.linkrel;

import internal.org.springframework.content.rest.support.DualContentRenditionEntity;
import org.jspecify.annotations.NonNull;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.fs.store.FileSystemContentStore;
import org.springframework.content.rest.RestResource;
import org.springframework.content.rest.StoreRestResource;

import java.io.InputStream;
import java.util.UUID;

@StoreRestResource(path = "dualContentRenditionEntities", linkRel = "foo")
public interface LinkRelDualContentRenditionEntityStore
    extends FileSystemContentStore<DualContentRenditionEntity, UUID> {

    @RestResource(exported = false, paths = {"rendition"})
    @Override
    InputStream getContent(@NonNull DualContentRenditionEntity entity, @NonNull PropertyPath propertyPath);
}
