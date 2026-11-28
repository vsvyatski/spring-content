package org.springframework.content.commons.renditions;

import java.io.InputStream;

public interface RenditionService {
    boolean canConvert(String fromMimeType, String toMimeType);

    InputStream convert(InputStream fromInputSource, String fromMimeType, String toMimeType);
}
