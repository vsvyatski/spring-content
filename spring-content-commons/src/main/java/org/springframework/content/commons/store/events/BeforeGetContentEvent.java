package org.springframework.content.commons.store.events;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.Store;

import java.io.Serial;
import java.io.Serializable;

public class BeforeGetContentEvent extends StoreEvent {
    @Serial
    private static final long serialVersionUID = 7544134720869656455L;

    public BeforeGetContentEvent(Object source, Store<Serializable> store) {
        super(source, store);
    }

    public BeforeGetContentEvent(Object source, PropertyPath propertyPath, Store<Serializable> store) {
        super(source, propertyPath, store);
    }
}
