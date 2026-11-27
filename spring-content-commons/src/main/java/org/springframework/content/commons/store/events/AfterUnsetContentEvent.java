package org.springframework.content.commons.store.events;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.Store;

import java.io.Serial;
import java.io.Serializable;

public class AfterUnsetContentEvent extends AfterStoreEvent {
    @Serial
    private static final long serialVersionUID = -4048489599363483802L;

    public AfterUnsetContentEvent(Object source, Store<Serializable> store) {
        super(source, store);
    }

    public AfterUnsetContentEvent(Object source, PropertyPath propertyPath, Store<Serializable> store) {
        super(source, propertyPath, store);
    }
}
