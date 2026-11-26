package org.springframework.content.commons.store;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.Resource;

import java.io.Serializable;

@NullMarked
public interface Store<SID extends Serializable> {

    /**
     * Returns a resource handle for the specified id.
     *
     * @param id the id of the resource
     * @return resource
     */
    @Nullable
    Resource getResource(SID id);
}
