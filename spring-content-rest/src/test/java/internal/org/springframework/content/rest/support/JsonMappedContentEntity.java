package internal.org.springframework.content.rest.support;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;
import org.springframework.content.test.support.UuidSingleContentFields;

import java.util.ArrayList;
import java.util.List;

@Entity
public class JsonMappedContentEntity extends UuidSingleContentFields implements ContentEntity {

    public @Id
    @GeneratedValue
    Long id;

    public String name;

    public @MimeType
    String mimeType;

    @JsonIgnore
    private String hidden;

    @JsonProperty("ying")
    private String yang;

    @OriginalFileName
    private String originalFileName;

    private String title;

    private List<String> things = new ArrayList<>();

    @OneToOne
    @JoinColumn(name = "testEntity4", nullable = true)
    @JsonProperty("testEntity4")
    private OneToOneLinkedContentEntity linkedContentEntity;

    public JsonMappedContentEntity() {
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

    public String getHidden() {
        return hidden;
    }

    public void setHidden(String hidden) {
        this.hidden = hidden;
    }

    public String getYang() {
        return yang;
    }

    public void setYang(String yang) {
        this.yang = yang;
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

    public List<String> getThings() {
        return things;
    }

    public void setThings(List<String> things) {
        this.things = things;
    }

    public OneToOneLinkedContentEntity getLinkedContentEntity() {
        return linkedContentEntity;
    }

    public void setLinkedContentEntity(OneToOneLinkedContentEntity linkedContentEntity) {
        this.linkedContentEntity = linkedContentEntity;
    }
}
