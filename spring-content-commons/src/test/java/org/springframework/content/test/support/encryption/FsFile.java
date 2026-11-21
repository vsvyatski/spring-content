package org.springframework.content.test.support.encryption;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.test.support.UuidSingleContentFields;

/**
 * JPA entity for encrypting store REST ITs (UUID content id, per-entity content encryption key). S3 ITs used to
 * name this type {@code File}; FS ITs {@code FsFile} — same shape, shared fixture.
 *
 * @author Vladimir Svyatski
 */
@Entity
public class FsFile extends UuidSingleContentFields {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name;

    @JsonIgnore
    private byte[] contentKey;

    @MimeType
    private String mimeType;

    public FsFile() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getContentKey() {
        return contentKey;
    }

    public void setContentKey(byte[] contentKey) {
        this.contentKey = contentKey;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
}