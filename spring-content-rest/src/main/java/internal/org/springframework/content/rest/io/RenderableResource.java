package internal.org.springframework.content.rest.io;

import org.springframework.util.MimeType;

import java.io.InputStream;

public interface RenderableResource {

    boolean isRenderableAs(MimeType mimeType);

    InputStream renderAs(MimeType mimeType);
}
