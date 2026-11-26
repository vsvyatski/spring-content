package org.springframework.content.commons.store;

import org.jspecify.annotations.NonNull;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.core.io.Resource;
import org.springframework.versions.LockParticipant;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

public interface ContentStore<S, SID extends Serializable> extends AssociativeStore<S, SID> {

    @LockParticipant
    S setContent(@NonNull S entity, InputStream content);

    @LockParticipant
    S setContent(@NonNull S entity, PropertyPath propertyPath, InputStream content);

    @LockParticipant
    S setContent(@NonNull S entity, PropertyPath propertyPath, InputStream content, long contentLen);

    @LockParticipant
    S setContent(@NonNull S entity, PropertyPath propertyPath, InputStream content, SetContentParams params);

    @LockParticipant
    S setContent(@NonNull S entity, Resource resourceContent);

    @LockParticipant
    S setContent(@NonNull S entity, PropertyPath propertyPath, Resource resourceContent);

    @LockParticipant
    S unsetContent(@NonNull S entity);

    @LockParticipant
    S unsetContent(@NonNull S entity, PropertyPath propertyPath);

    @LockParticipant
    S unsetContent(@NonNull S entity, PropertyPath propertyPath, UnsetContentParams params);

    InputStream getContent(@NonNull S entity) throws IOException;

    InputStream getContent(@NonNull S entity, PropertyPath propertyPath) throws IOException;
}
