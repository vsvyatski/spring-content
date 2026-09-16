package org.springframework.content.commons.repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.core.io.Resource;
import org.springframework.versions.LockParticipant;

/**
 * @deprecated This interface is deprecated. Use {@link org.springframework.content.commons.store.ContentStore}
 * instead.
 */
@Deprecated
public interface ContentStore<S, SID extends Serializable> extends AssociativeStore<S, SID>, ContentRepository {

    @LockParticipant
    S setContent(S entity, InputStream content);

    @LockParticipant
    S setContent(S entity, PropertyPath propertyPath, InputStream content);

    @LockParticipant
    S setContent(S entity, PropertyPath propertyPath, InputStream content, long contentLen);

    @LockParticipant
    S setContent(S entity, PropertyPath propertyPath, InputStream content, SetContentParams params);

    @LockParticipant
    S setContent(S entity, Resource resourceContent);

    @LockParticipant
    S setContent(S entity, PropertyPath propertyPath, Resource resourceContent);

    @LockParticipant
    S unsetContent(S entity);

    @LockParticipant
    S unsetContent(S entity, PropertyPath propertyPath);

    @LockParticipant
    S unsetContent(S entity, PropertyPath propertyPath, UnsetContentParams params);

    InputStream getContent(S entity) throws IOException;

    InputStream getContent(S entity, PropertyPath propertyPath) throws IOException;
}
