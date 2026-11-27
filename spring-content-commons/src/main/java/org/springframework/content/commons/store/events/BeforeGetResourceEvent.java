package org.springframework.content.commons.store.events;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.Store;

import java.io.Serial;
import java.io.Serializable;

public class BeforeGetResourceEvent extends StoreEvent {
    @Serial
    private static final long serialVersionUID = 4837374805325538391L;

    public BeforeGetResourceEvent(Object source, Store<Serializable> store) {
        super(source, store);
    }

    public BeforeGetResourceEvent(Object source, PropertyPath propertyPath, Store<Serializable> store) {
        super(source, propertyPath, store);
    }
}
