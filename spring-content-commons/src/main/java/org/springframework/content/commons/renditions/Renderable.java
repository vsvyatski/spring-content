package org.springframework.content.commons.renditions;

import org.springframework.content.commons.property.PropertyPath;

import java.io.InputStream;

public interface Renderable<S> {

    boolean hasRendition(S entity, String mimeType);

    boolean hasRendition(S entity, PropertyPath path, String mimeType);

	InputStream getRendition(S entity, String mimeType);

	InputStream getRendition(S entity, PropertyPath path, String mimeType);
}
