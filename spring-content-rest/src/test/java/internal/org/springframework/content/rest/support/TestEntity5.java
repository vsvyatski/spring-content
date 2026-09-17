package internal.org.springframework.content.rest.support;

import java.util.Date;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
public class TestEntity5 {
	public @Id @GeneratedValue Long id;

	public String name;

	private @ContentId UUID contentId;
	private @ContentLength Long contentLen;
	private @MimeType String contentMimeType;

	private @ContentId UUID renditionId;
	private @ContentLength Long renditionLen;
	private @MimeType String renditionMimeType;

	private @Version Long version;
	private @CreatedDate Date createdDate;
	private @LastModifiedDate Date modifiedDate;

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

	public UUID getRenditionId() {
		return renditionId;
	}

	public void setRenditionId(UUID renditionId) {
		this.renditionId = renditionId;
	}

	public Long getRenditionLen() {
		return renditionLen;
	}

	public void setRenditionLen(Long renditionLen) {
		this.renditionLen = renditionLen;
	}

	public String getRenditionMimeType() {
		return renditionMimeType;
	}

	public void setRenditionMimeType(String renditionMimeType) {
		this.renditionMimeType = renditionMimeType;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	public Date getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

	public Date getModifiedDate() {
		return modifiedDate;
	}

	public void setModifiedDate(Date modifiedDate) {
		this.modifiedDate = modifiedDate;
	}
}
