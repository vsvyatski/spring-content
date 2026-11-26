package internal.org.springframework.content.rest.io;

import org.springframework.content.commons.io.RangeableResource;
import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.core.io.WritableResource;

public interface AssociatedStoreResource<S> extends WritableResource, StoreResource, RangeableResource {

    S getAssociation();

    PropertyPath getPropertyPath();

    ContentProperty getContentProperty();

    /**
     * Whether the entity carries a content id for this property (metadata association), regardless of whether bytes
     * exist in storage.
     */
    boolean isAssociated();
}
