package internal.org.springframework.content.rest.support;

import jakarta.persistence.Embeddable;
import org.hibernate.annotations.Formula;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;
import org.springframework.content.test.support.UuidSingleContentFields;

import java.util.UUID;

@Embeddable
public class AuditedNestedContentChild extends UuidSingleContentFields {

    @MimeType
    private String mimeType;

    @OriginalFileName
    private String fileName = "";

    // prevent EmbeddedContentChild from being return by hibernate as null
    @Formula("1")
    private int workaroundForBraindeadJpaImplementation;

    public AuditedNestedContentChild() {
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
