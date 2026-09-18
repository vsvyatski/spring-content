package org.springframework.content.commons.store.events;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.Store;
import org.springframework.context.ApplicationEvent;

import java.io.Serial;
import java.io.Serializable;

public class StoreEvent extends ApplicationEvent {
    @Serial
    private static final long serialVersionUID = -4985896308323075130L;

    private final Store<Serializable> store;
    private PropertyPath propertyPath;

    public StoreEvent(Object source, Store<Serializable> store) {
        super(source);
        this.store = store;
    }

    public StoreEvent(Object source, PropertyPath propertyPath, Store<Serializable> store) {
        super(source);
        this.propertyPath = propertyPath;
        this.store = store;
    }

    public PropertyPath getPropertyPath() {
        return propertyPath;
    }

    public Store<Serializable> getStore() {
        return store;
    }
}
