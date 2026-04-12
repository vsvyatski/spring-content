package org.springframework.content.commons.repository.factory.testsupport;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.*;
import org.springframework.content.commons.store.factory.AbstractStoreFactoryBean;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.io.Serializable;

public class TestStoreFactoryBean extends AbstractStoreFactoryBean {

    public TestStoreFactoryBean(Class<? extends Store<Serializable>> storeInterface) {
        super(storeInterface);
    }

    @Override
    protected Object getContentStoreImpl() {
        return new TestConfigStoreImpl();
    }

    public static class TestConfigStoreImpl implements ContentStore<Object, Serializable> {
        @Override
        public Object setContent(Object property, InputStream content) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object setContent(Object property, Resource resourceContent) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object unsetContent(Object property) {
            throw new UnsupportedOperationException();
        }

        @Override
        public InputStream getContent(Object property) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Resource getResource(Object entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void associate(Object entity, Serializable id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void unassociate(Object entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Resource getResource(Serializable id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Resource getResource(Object entity, PropertyPath propertyPath) {
            return null;
        }

        @Override
        public Resource getResource(Object entity, PropertyPath propertyPath, GetResourceParams params) {
            return null;
        }

        @Override
        public void associate(Object entity, PropertyPath propertyPath, Serializable id) {
        }

        @Override
        public void unassociate(Object entity, PropertyPath propertyPath) {
        }

        @Override
        public Object setContent(Object property, PropertyPath propertyPath, InputStream content) {
            return null;
        }

        @Override
        public Object setContent(Object property, PropertyPath propertyPath, InputStream content, long contentLen) {
            return null;
        }

        @Override
        public Object setContent(Object entity, PropertyPath propertyPath, InputStream content, SetContentParams params) {
            return null;
        }

        @Override
        public Object setContent(Object property, PropertyPath propertyPath, Resource resourceContent) {
            return null;
        }

        @Override
        public Object unsetContent(Object property, PropertyPath propertyPath) {
            return null;
        }

        @Override
        public Object unsetContent(Object entity, PropertyPath propertyPath, UnsetContentParams params) {
            return null;
        }

        @Override
        public InputStream getContent(Object property, PropertyPath propertyPath) {
            return null;
        }
    }
}
