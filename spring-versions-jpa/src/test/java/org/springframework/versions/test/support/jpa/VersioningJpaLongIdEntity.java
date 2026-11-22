package org.springframework.versions.test.support.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import org.springframework.versions.*;

/**
 * JPA entity with non-default property names for {@link LockingAndVersioningRepository} ITs.
 *
 * @author Vladimir Svyatski
 */
@Entity
@SuppressWarnings("unused")
public class VersioningJpaLongIdEntity {

    @Id
    @GeneratedValue
    private Long xid;
    @Version
    private Long version;
    @AncestorId
    private Long xAncestorId;
    @AncestorRootId
    private Long xAncestorRootId;
    @SuccessorId
    private Long xSuccessorId;
    @LockOwner
    private String xLockOwner;
    @VersionNumber
    private String versionNo;
    @VersionLabel
    private String versionLabel;

    public VersioningJpaLongIdEntity() {
    }

    public Long getXid() {
        return xid;
    }

    public void setXid(Long xid) {
        this.xid = xid;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Long getXAncestorId() {
        return xAncestorId;
    }

    public void setXAncestorId(Long xAncestorId) {
        this.xAncestorId = xAncestorId;
    }

    public Long getXAncestorRootId() {
        return xAncestorRootId;
    }

    public void setXAncestorRootId(Long xAncestorRootId) {
        this.xAncestorRootId = xAncestorRootId;
    }

    public Long getXSuccessorId() {
        return xSuccessorId;
    }

    public void setXSuccessorId(Long xSuccessorId) {
        this.xSuccessorId = xSuccessorId;
    }

    public String getXLockOwner() {
        return xLockOwner;
    }

    public void setXLockOwner(String xLockOwner) {
        this.xLockOwner = xLockOwner;
    }

    public String getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(String versionNo) {
        this.versionNo = versionNo;
    }

    public String getVersionLabel() {
        return versionLabel;
    }

    public void setVersionLabel(String versionLabel) {
        this.versionLabel = versionLabel;
    }
}
