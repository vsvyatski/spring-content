package org.springframework.content.commons.renditions;

import java.io.InputStream;

public interface RenditionService {
    boolean canConvert(String fromMimeType, String toMimeType);

    InputStream convert(String fromMimeType, InputStream fromInputSource, String toMimeType);
}
