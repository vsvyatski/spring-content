package org.springframework.content.encryption.store;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.GetResourceParams;
import org.springframework.content.commons.store.SetContentParams;
import org.springframework.content.commons.store.UnsetContentParams;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

@NullMarked
public interface EncryptingContentStore<S> {

    @Nullable
    InputStream getContent(S entity, PropertyPath propertyPath) throws IOException;

    S setContent(S entity, PropertyPath propertyPath, InputStream content);

    S setContent(S entity, PropertyPath propertyPath, InputStream content, long contentLen);

    S setContent(S entity, PropertyPath propertyPath, InputStream content, SetContentParams params);

    S setContent(S entity, PropertyPath propertyPath, Resource resourceContent);

    S unsetContent(S entity, PropertyPath propertyPath);

    S unsetContent(S entity, PropertyPath propertyPath, UnsetContentParams params);

    @Nullable
    Resource getResource(S entity, PropertyPath propertyPath);

    @Nullable
    Resource getResource(S entity, PropertyPath propertyPath, GetResourceParams params);
}
