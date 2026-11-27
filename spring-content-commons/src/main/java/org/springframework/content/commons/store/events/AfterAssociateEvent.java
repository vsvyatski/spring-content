package org.springframework.content.commons.store.events;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.Store;

import java.io.Serial;
import java.io.Serializable;

public class AfterAssociateEvent extends AfterStoreEvent {
    @Serial
    private static final long serialVersionUID = -2476794889567520628L;

    public AfterAssociateEvent(Object source, Store<Serializable> store) {
        super(source, store);
    }

    public AfterAssociateEvent(Object source, PropertyPath propertyPath, Store<Serializable> store) {
        super(source, propertyPath, store);
    }
}
