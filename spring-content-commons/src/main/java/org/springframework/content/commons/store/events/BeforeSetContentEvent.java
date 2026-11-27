package org.springframework.content.commons.store.events;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.Store;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.io.Serial;
import java.io.Serializable;

public class BeforeSetContentEvent extends StoreEvent {
    @Serial
    private static final long serialVersionUID = -7299354365313770L;

    private InputStream inputStream;
    private Resource resource;

    public BeforeSetContentEvent(Object source, Store<Serializable> store, InputStream is) {
        super(source, store);
        this.inputStream = is;
    }

    public BeforeSetContentEvent(Object source, PropertyPath propertyPath, Store<Serializable> store, InputStream is) {
        super(source, propertyPath, store);
        this.inputStream = is;
    }

    public BeforeSetContentEvent(Object source, PropertyPath propertyPath, Store<Serializable> store, Resource resource) {
        super(source, propertyPath, store);
        this.resource = resource;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream is) {
        this.inputStream = is;
    }

    public Resource getResource() {
        return resource;
    }
}
