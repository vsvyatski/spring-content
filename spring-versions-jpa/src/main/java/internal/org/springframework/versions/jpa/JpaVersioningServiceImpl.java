package internal.org.springframework.versions.jpa;

import jakarta.persistence.Id;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.versions.*;

public class JpaVersioningServiceImpl implements VersioningService {

    @Override
    public Object establishAncestralRoot(Object entity) {
        Object id = getId(entity);

        if (BeanUtils.hasAnnotatedField(entity, AncestorId.class)) {
            BeanUtils.setAnnotatedValue(entity, AncestorId.class, null);
        }

        if (BeanUtils.hasAnnotatedField(entity, AncestorRootId.class) && BeanUtils.getAnnotatedValue(entity, AncestorRootId.class) == null) {
            BeanUtils.setAnnotatedValue(entity, AncestorRootId.class, id);
        }

        return entity;
    }

    @Override
    public Object establishAncestor(Object entity, Object successor) {

        if (BeanUtils.hasAnnotatedField(entity, SuccessorId.class)) {
            Object successorId = getId(successor);
            BeanUtils.setAnnotatedValue(entity, SuccessorId.class, successorId);
        }

        return entity;
    }

    @Override
    public Object establishSuccessor(Object candidate, String versionNo, String versionLabel,
                                     Object ancestralRoot, Object ancestor) {
        BeanUtils.setAnnotatedValue(candidate, VersionNumber.class, versionNo);
        BeanUtils.setAnnotatedValue(candidate, VersionLabel.class, versionLabel);

        if (BeanUtils.hasAnnotatedField(candidate, AncestorRootId.class)) {
            Object ancestralRootId = getId(ancestralRoot);
            BeanUtils.setAnnotatedValue(candidate, AncestorRootId.class, ancestralRootId);
        }

        if (BeanUtils.hasAnnotatedField(candidate, AncestorId.class) && ancestor != null) {
            Object ancestorId = getId(ancestor);
            BeanUtils.setAnnotatedValue(candidate, AncestorId.class, ancestorId);
        }

        return candidate;
    }

    protected Object getId(Object entity) {
        Object id = BeanUtils.getAnnotatedValue(entity, Id.class);
        if (id == null) {
            id = BeanUtils.getAnnotatedValue(entity, org.springframework.data.annotation.Id.class);
        }
        return id;
    }
}
