package internal.org.springframework.content.rest.support;

import org.springframework.content.test.support.ContentAccessor;

import java.util.UUID;

/**
 * REST IT entities with a long surrogate key and UUID {@linkplain org.springframework.content.commons.annotations.ContentId}.
 *
 * @author Vladimir Svyatski
 */
public interface ContentEntity extends ContentAccessor<UUID> {

    Long getId();

    void setId(Long id);

    default Long getLen() {
        return getContentLen();
    }

    default void setLen(Long len) {
        setContentLen(len);
    }

    String getMimeType();

    void setMimeType(String mimeType);

    String getOriginalFileName();

    void setOriginalFileName(String originalFileName);

    String getTitle();

    void setTitle(String title);
}
