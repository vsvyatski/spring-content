package internal.org.springframework.content.rest.scoped.http405;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.SetContentParams;
import org.springframework.content.commons.store.UnsetContentParams;
import org.springframework.content.fs.store.FileSystemContentStore;
import org.springframework.content.rest.RestResource;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.util.UUID;

/**
 * @author Vladimir Svyatski
 */
@SuppressWarnings("unused") // classpath / store component scan
public interface UnexportedStoreMethodsEntityStore extends FileSystemContentStore<UnexportedStoreMethodsEntity, UUID> {

    @RestResource(exported = false)
    @Override
    InputStream getContent(UnexportedStoreMethodsEntity property, PropertyPath path);

    @RestResource(exported = false)
    @Override
    UnexportedStoreMethodsEntity setContent(UnexportedStoreMethodsEntity property, PropertyPath path, InputStream content);

    @RestResource(exported = false)
    @Override
    UnexportedStoreMethodsEntity setContent(UnexportedStoreMethodsEntity property, PropertyPath path, InputStream content,
        SetContentParams params);

    @RestResource(exported = false)
    @Override
    UnexportedStoreMethodsEntity setContent(UnexportedStoreMethodsEntity property, PropertyPath path,
        Resource resourceContent);

    @RestResource(exported = false)
    @Override
    UnexportedStoreMethodsEntity unsetContent(UnexportedStoreMethodsEntity property, PropertyPath path,
        UnsetContentParams params);
}