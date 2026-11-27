package org.springframework.content.commons.store.events;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.Store;

import java.io.Serial;
import java.io.Serializable;

public class BeforeAssociateEvent extends StoreEvent {
    @Serial
    private static final long serialVersionUID = -200025429354484417L;

    public BeforeAssociateEvent(Object source, Store<Serializable> store) {
        super(source, store);
    }

    public BeforeAssociateEvent(Object source, PropertyPath propertyPath, Store<Serializable> store) {
        super(source, propertyPath, store);
    }
}
