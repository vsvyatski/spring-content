package internal.org.springframework.content.rest.support;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;
import org.springframework.content.test.support.UuidSingleContentFields;

@Entity
public class UuidSingleContentEntity extends UuidSingleContentFields implements ContentEntity {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    @MimeType
    private String mimeType;

    @OriginalFileName
    private String originalFileName;

    private String title;

    public UuidSingleContentEntity() {
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    @Override
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    @Override
    public String getOriginalFileName() {
        return originalFileName;
    }

    @Override
    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }
}
