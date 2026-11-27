package org.springframework.content.commons.store.events;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.Store;

import java.io.Serial;
import java.io.Serializable;

public class AfterGetResourceEvent extends AfterStoreEvent {
    @Serial
    private static final long serialVersionUID = -6543269701121386583L;

    public AfterGetResourceEvent(Object source, Store<Serializable> store) {
        super(source, store);
    }

    public AfterGetResourceEvent(Object source, PropertyPath propertyPath, Store<Serializable> store) {
        super(source, propertyPath, store);
    }
}
