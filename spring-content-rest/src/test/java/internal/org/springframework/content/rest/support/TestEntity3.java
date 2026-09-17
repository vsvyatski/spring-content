package internal.org.springframework.content.rest.support;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
public class TestEntity3 implements ContentEntity {
	public @Id @GeneratedValue Long id;
	public String name;
	@JsonIgnore private String hidden;
	@JsonProperty("ying") private String yang;
	public @ContentId UUID contentId;
	public @ContentLength Long len;
	public @MimeType String mimeType;
	private @OriginalFileName String originalFileName;
	private String title;

	private List<String> things = new ArrayList<>();

	@OneToOne
	@JoinColumn(name = "testEntity4", nullable = true)
	private TestEntity4 testEntity4;

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

	public UUID getContentId() {
		return contentId;
	}

	public void setContentId(UUID contentId) {
		this.contentId = contentId;
	}

	public Long getLen() {
		return len;
	}

	public void setLen(Long len) {
		this.len = len;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public String getOriginalFileName() {
		return originalFileName;
	}

	public void setOriginalFileName(String originalFileName) {
		this.originalFileName = originalFileName;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public List<String> getThings() {
		return things;
	}

	public void setThings(List<String> things) {
		this.things = things;
	}

	public TestEntity4 getTestEntity4() {
		return testEntity4;
	}

	public void setTestEntity4(TestEntity4 testEntity4) {
		this.testEntity4 = testEntity4;
	}
}
