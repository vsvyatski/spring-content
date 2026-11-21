package internal.org.springframework.content.rest.support;

import org.springframework.content.test.support.ContentAccessor;

import java.util.UUID;

public interface ContentEntity extends ContentAccessor<UUID> {

    Long getId();

    void setId(Long id);

    @Override
    default Long getContentLen() {
        return getLen();
    }

    @Override
    default void setContentLen(Long contentLen) {
        setLen(contentLen);
    }

    Long getLen();

    void setLen(Long len);

    String getMimeType();

    void setMimeType(String mimeType);

    String getOriginalFileName();

    void setOriginalFileName(String originalFileName);

    String getTitle();

    void setTitle(String title);
}
