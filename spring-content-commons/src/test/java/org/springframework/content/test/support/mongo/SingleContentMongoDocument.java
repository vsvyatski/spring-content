package org.springframework.content.test.support.mongo;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.data.annotation.Id;

/**
 * Minimal Mongo document for Enable-store configuration tests.
 *
 * @author Vladimir Svyatski
 */
public class SingleContentMongoDocument {

    @Id
    private String id;

    @ContentId
    private String contentId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }
}