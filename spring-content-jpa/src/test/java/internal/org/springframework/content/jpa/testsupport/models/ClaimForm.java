package internal.org.springframework.content.jpa.testsupport.models;

import jakarta.persistence.Embeddable;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;

@Embeddable
public class ClaimForm {

	@ContentId
	private String contentId;

	@ContentLength
	private Long contentLength = 0L;

	@MimeType
	private String contentMimeType = "text/plain";

    @ContentId
    private String renditionId;

    @ContentLength
    private long renditionLen;

	public String getContentId() {
		return contentId;
	}

	public void setContentId(String contentId) {
		this.contentId = contentId;
	}

	public Long getContentLength() {
		return contentLength;
	}

	public void setContentLength(Long contentLength) {
		this.contentLength = contentLength;
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

	// Ensure we can handle entities with "computed" getters; i.e. getters that
	// dont have an associated field
	public boolean getIsActive() {
		return true;
	}
}
