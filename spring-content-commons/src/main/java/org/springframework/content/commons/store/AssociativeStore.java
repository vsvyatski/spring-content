package org.springframework.content.commons.store;

import org.jspecify.annotations.NonNull;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.core.io.Resource;

import java.io.Serializable;

public interface AssociativeStore<S, SID extends Serializable> extends Store<SID> {

    /**
     * Returns the resource associated with the given entity, or null if no association exists
     *
     * @param entity the entity associated with resource
     * @return resource
     */
    Resource getResource(@NonNull S entity);

    /**
     * Returns the resource associated with the given property for entity, or null if no association exists
     *
     * @param entity       the entity associated with resource
     * @param propertyPath the property path of the associated resource
     * @return resource
     */
    Resource getResource(@NonNull S entity, @NonNull PropertyPath propertyPath);

    /**
     * Returns the resource associated with the given property for entity, or null if no association exists
     *
     * @param entity       the entity associated with resource
     * @param propertyPath the property path of the associated resource
     * @param params       parameters customizing how the resource is fetched and returned
     * @return resource
     */
    Resource getResource(@NonNull S entity, @NonNull PropertyPath propertyPath, @NonNull GetResourceParams params);

    /**
     * Associates the resource (identified by id) with entity.
     *
     * @param entity the target of the association
     * @param id     the id of the resource to be associated
     */
    void associate(@NonNull S entity, SID id);

    /**
     * Associates the resource (identified by id) with entity.
     *
     * @param entity       the target of the association
     * @param propertyPath the property path to associate the resource to
     * @param id           the id of the resource to be associated
     */
    void associate(@NonNull S entity, @NonNull PropertyPath propertyPath, SID id);

    /**
     * Unassociates the resource from entity
     *
     * @param entity the target of the un-association
     */
    void unassociate(@NonNull S entity);

    /**
     * Unassociates the resource from entity
     *
     * @param entity       the target of the un-association
     * @param propertyPath the property path to unassociate the resource from
     */
    void unassociate(@NonNull S entity, @NonNull PropertyPath propertyPath);
}
