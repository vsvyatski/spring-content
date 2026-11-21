package org.springframework.content.test.support.mongo;

import org.springframework.content.test.support.SingleContentFields;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Minimal Mongo document for Enable-store configuration tests.
 *
 * @author Vladimir Svyatski
 */
@Document
public class SingleContentMongoDocument extends SingleContentFields {

    @Id
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
