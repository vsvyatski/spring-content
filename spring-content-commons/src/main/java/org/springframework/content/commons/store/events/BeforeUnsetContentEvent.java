package org.springframework.content.commons.store.events;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.Store;

import java.io.Serial;
import java.io.Serializable;

public class BeforeUnsetContentEvent extends StoreEvent {
    @Serial
    private static final long serialVersionUID = -942044847341579940L;

    public BeforeUnsetContentEvent(Object source, Store<Serializable> store) {
        super(source, store);
    }

    public BeforeUnsetContentEvent(Object source, PropertyPath propertyPath, Store<Serializable> store) {
        super(source, propertyPath, store);
    }
}
