package internal.org.springframework.content.rest.support;

import java.util.UUID;

import jakarta.persistence.Embeddable;

import org.hibernate.annotations.Formula;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;

@Embeddable
public class TestEntity10Child {

    public TestEntity10Child() {
    }

    @ContentId public UUID contentId;
	@ContentLength public Long contentLen;
	@MimeType public String contentMimeType;
	@OriginalFileName public String contentFileName = "";

    @ContentId public UUID previewId;
    @ContentLength public Long previewLen;
    @MimeType public String previewMimeType;

	// prevent TestEntity8Child from being return by hibernate as null
	@Formula("1")
	private int workaroundForBraindeadJpaImplementation;

	public UUID getContentId() {
		return contentId;
	}

	public void setContentId(UUID contentId) {
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

	public String getContentFileName() {
		return contentFileName;
	}

	public void setContentFileName(String contentFileName) {
		this.contentFileName = contentFileName;
	}

	public UUID getPreviewId() {
		return previewId;
	}

	public void setPreviewId(UUID previewId) {
		this.previewId = previewId;
	}

	public Long getPreviewLen() {
		return previewLen;
	}

	public void setPreviewLen(Long previewLen) {
		this.previewLen = previewLen;
	}

	public String getPreviewMimeType() {
		return previewMimeType;
	}

	public void setPreviewMimeType(String previewMimeType) {
		this.previewMimeType = previewMimeType;
	}

	public int getWorkaroundForBraindeadJpaImplementation() {
		return workaroundForBraindeadJpaImplementation;
	}

	public void setWorkaroundForBraindeadJpaImplementation(int workaroundForBraindeadJpaImplementation) {
		this.workaroundForBraindeadJpaImplementation = workaroundForBraindeadJpaImplementation;
	}
}
