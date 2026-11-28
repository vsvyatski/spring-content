package org.springframework.content.commons.renditions;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.content.commons.property.PropertyPath;

import java.io.InputStream;

@NullMarked
public interface Renderable<S> {

    boolean hasRendition(S entity, String mimeType);

    boolean hasRendition(S entity, PropertyPath path, String mimeType);

    @Nullable
    InputStream getRendition(S entity, String mimeType);

    @Nullable
    InputStream getRendition(S entity, PropertyPath path, String mimeType);
}
