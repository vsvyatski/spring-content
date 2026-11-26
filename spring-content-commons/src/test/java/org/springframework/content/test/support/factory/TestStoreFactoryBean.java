package org.springframework.content.test.support.factory;

import org.jspecify.annotations.NonNull;
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
        public Object setContent(@NonNull Object property, InputStream content) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object setContent(@NonNull Object property, Resource resourceContent) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object unsetContent(@NonNull Object property) {
            throw new UnsupportedOperationException();
        }

        @Override
        public InputStream getContent(@NonNull Object property) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Resource getResource(@NonNull Object entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void associate(@NonNull Object entity, Serializable id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void unassociate(@NonNull Object entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Resource getResource(Serializable id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Resource getResource(@NonNull Object entity, @NonNull PropertyPath propertyPath) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Resource getResource(@NonNull Object entity, @NonNull PropertyPath propertyPath, @NonNull GetResourceParams params) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void associate(@NonNull Object entity, @NonNull PropertyPath propertyPath, Serializable id) {
            // TODO Auto-generated method stub
        }

        @Override
        public void unassociate(@NonNull Object entity, @NonNull PropertyPath propertyPath) {
            // TODO Auto-generated method stub
        }

        @Override
        public Object setContent(@NonNull Object property, @NonNull PropertyPath propertyPath, InputStream content) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Object setContent(@NonNull Object property, @NonNull PropertyPath propertyPath, InputStream content, long contentLen) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Object setContent(@NonNull Object entity, @NonNull PropertyPath propertyPath, InputStream content, @NonNull SetContentParams params) {
            return null;
        }

        @Override
        public Object setContent(@NonNull Object property, @NonNull PropertyPath propertyPath, Resource resourceContent) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Object unsetContent(@NonNull Object property, @NonNull PropertyPath propertyPath) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Object unsetContent(@NonNull Object entity, @NonNull PropertyPath propertyPath, @NonNull UnsetContentParams params) {
            return null;
        }

        @Override
        public InputStream getContent(@NonNull Object property, @NonNull PropertyPath propertyPath) {
            // TODO Auto-generated method stub
            return null;
        }
    }
}
