package org.springframework.content.commons.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.core.io.Resource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static java.lang.String.format;

/**
 * Shared {@code ContentStore#getContent} behavior: return {@code null} when storage has no bytes, with warn logs when
 * metadata still references a content id.
 *
 * @author Vladimir Svyatski
 */
public final class ContentStoreGetContentSupport {

    private static final Log logger = LogFactory.getLog(ContentStoreGetContentSupport.class);

    private ContentStoreGetContentSupport() {
    }

    public static String propertyPathLabel(PropertyPath propertyPath) {
        return propertyPath == null ? "content" : propertyPath.toString();
    }

    public static InputStream openContentStream(Object entity, String contentPropertyLabel, Object contentId,
                                                Resource resource, String backingDescription) {
        try {
            if (resource == null || !resource.exists()) {
                logger.warn(format(
                    "Content id %s on entity %s (%s) has no backing %s",
                    contentId, entity, contentPropertyLabel, backingDescription));
                return null;
            }
            try {
                return resource.getInputStream();
            } catch (FileNotFoundException e) {
                logger.warn(format(
                    "Content id %s on entity %s (%s) is present in metadata but %s bytes are missing",
                    contentId, entity, contentPropertyLabel, backingDescription), e);
                return null;
            }
        } catch (IOException e) {
            logger.error(format("Unexpected error getting content for entity %s", entity), e);
            throw new StoreAccessException(format("Getting content for entity %s", entity), e);
        }
    }
}
