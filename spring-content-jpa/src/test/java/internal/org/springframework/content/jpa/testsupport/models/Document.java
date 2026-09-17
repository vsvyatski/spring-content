package internal.org.springframework.content.jpa.testsupport.models;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;

@Entity
public class Document {

    @Id
    @org.springframework.data.annotation.Id
    private String id = UuidCreator.getTimeOrdered().toString();

    @ContentId
    private String contentId;

    @ContentLength
    private Long contentLen;

    @MimeType
    private String contentMimeType;

    @ContentId
    private String renditionId;

    @ContentLength
    private long renditionLen;

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

    public Long getContentLen() {
        return contentLen;
    }

    public void setContentLen(Long contentLen) {
        this.contentLen = contentLen;
    }

    public String getContentMimeType() {
        return contentMimeType;
    }

    public void setContentMimeType(String contentMimeType) {
        this.contentMimeType = contentMimeType;
    }

    public String getRenditionId() {
        return renditionId;
    }

    public void setRenditionId(String renditionId) {
        this.renditionId = renditionId;
    }

    public long getRenditionLen() {
        return renditionLen;
    }

    public void setRenditionLen(long renditionLen) {
        this.renditionLen = renditionLen;
    }
}
