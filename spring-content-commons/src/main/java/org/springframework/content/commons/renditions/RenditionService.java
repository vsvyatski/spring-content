package org.springframework.content.commons.renditions;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;

@NullMarked
public interface RenditionService {
    boolean canConvert(String fromMimeType, String toMimeType);

    @Nullable
    InputStream convert(InputStream source, String fromMimeType, String toMimeType);
}
