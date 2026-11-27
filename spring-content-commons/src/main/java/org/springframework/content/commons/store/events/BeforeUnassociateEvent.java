package org.springframework.content.commons.store.events;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.Store;

import java.io.Serial;
import java.io.Serializable;

public class BeforeUnassociateEvent extends StoreEvent {
    @Serial
    private static final long serialVersionUID = 3943304832854546581L;

    public BeforeUnassociateEvent(Object source, Store<Serializable> store) {
        super(source, store);
    }

    public BeforeUnassociateEvent(Object source, PropertyPath propertyPath, Store<Serializable> store) {
        super(source, propertyPath, store);
    }
}
