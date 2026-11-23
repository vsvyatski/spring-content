package org.springframework.versions.test.support;

import jakarta.persistence.Id;
import org.springframework.versions.*;

/**
 * Non-JPA POJO for {@link internal.org.springframework.versions.jpa.JpaVersioningServiceImpl} unit tests.
 *
 * @author Vladimir Svyatski
 */
public class VersioningUnitFields {

    @Id
    private Long id;
    @AncestorId
    private Long ancestorId;
    @AncestorRootId
    private Long ancestorRootId;
    @SuccessorId
    private Long successorId;
    @VersionNumber
    private String versionNo;
    @VersionLabel
    private String versionLabel;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAncestorId() {
        return ancestorId;
    }

    @SuppressWarnings("unused") // BeanWrapper write method for @AncestorId
    public void setAncestorId(Long ancestorId) {
        this.ancestorId = ancestorId;
    }

    public Long getAncestorRootId() {
        return ancestorRootId;
    }

    @SuppressWarnings("unused") // BeanWrapper write method for @AncestorRootId
    public void setAncestorRootId(Long ancestorRootId) {
        this.ancestorRootId = ancestorRootId;
    }

    public Long getSuccessorId() {
        return successorId;
    }

    @SuppressWarnings("unused") // BeanWrapper write method for @SuccessorId
    public void setSuccessorId(Long successorId) {
        this.successorId = successorId;
    }

    public String getVersionNo() {
        return versionNo;
    }

    @SuppressWarnings("unused") // BeanWrapper write method for @VersionNumber
    public void setVersionNo(String versionNo) {
        this.versionNo = versionNo;
    }

    public String getVersionLabel() {
        return versionLabel;
    }

    @SuppressWarnings("unused") // BeanWrapper write method for @VersionLabel
    public void setVersionLabel(String versionLabel) {
        this.versionLabel = versionLabel;
    }
}
