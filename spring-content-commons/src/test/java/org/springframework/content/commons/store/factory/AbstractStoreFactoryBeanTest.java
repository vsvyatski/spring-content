package org.springframework.content.commons.store.factory;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.*;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.io.Serializable;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractStoreFactoryBeanTest {

    public interface TestStore extends Serializable, ContentStore<String, UUID> {
    }

    public interface ContentStoreNotFirstInterfaceStore
        extends Serializable, ContentStore<String, UUID> {
    }

    public static class TestContentStoreFactory extends org.springframework.content.commons.store.factory.AbstractStoreFactoryBean {
        protected TestContentStoreFactory(Class<? extends Store<Serializable>> storeInterface) {
            super(storeInterface);
        }

        @Override
        protected Object getContentStoreImpl() {
            return new TestConfigStoreImpl();
        }
    }

    public static class TestConfigStoreImpl
        implements ContentStore<Object, Serializable> {

        @Override
        public Object setContent(@NonNull Object property, InputStream content) {
            return null;
        }

        @Override
        public Object setContent(@NonNull Object property, Resource resourceContent) {
            return null;
        }

        @Override
        public Object unsetContent(@NonNull Object property) {
            return null;
        }

        @Override
        public InputStream getContent(@NonNull Object property) {
            return null;
        }

        @Override
        public Resource getResource(@NonNull Object entity) {
            return null;
        }

        @Override
        public void associate(@NonNull Object entity, Serializable id) {
        }

        @Override
        public void unassociate(@NonNull Object entity) {
        }

        @Override
        public Resource getResource(Serializable id) {
            return null;
        }

        @Override
        public Resource getResource(@NonNull Object entity, PropertyPath propertyPath) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Resource getResource(@NonNull Object entity, PropertyPath propertyPath, GetResourceParams params) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void associate(@NonNull Object entity, PropertyPath propertyPath, Serializable id) {
            // TODO Auto-generated method stub
        }

        @Override
        public void unassociate(@NonNull Object entity, PropertyPath propertyPath) {
            // TODO Auto-generated method stub
        }

        @Override
        public Object setContent(@NonNull Object property, PropertyPath propertyPath, InputStream content) {
            // TODO Auto-generated method stub
            return null;
        }

        public Object setContent(@NonNull Object property, PropertyPath propertyPath, InputStream content, long contentLen) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Object setContent(@NonNull Object entity, PropertyPath propertyPath, InputStream content, SetContentParams params) {
            return null;
        }

        @Override
        public Object setContent(@NonNull Object property, PropertyPath propertyPath, Resource resourceContent) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Object unsetContent(@NonNull Object property, PropertyPath propertyPath) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Object unsetContent(@NonNull Object entity, PropertyPath propertyPath, UnsetContentParams params) {
            return null;
        }

        @Override
        public InputStream getContent(@NonNull Object property, PropertyPath propertyPath) {
            // TODO Auto-generated method stub
            return null;
        }
    }

    @Nested
    class AbstractContentStoreFactoryBean {
        @Nested
        class GetDomainClass {
            @Test
            void getsTheDomainClass() {
                Class<?> domainClass = AbstractStoreFactoryBean.getDomainClass(TestStore.class);
                assertThat(domainClass).isEqualTo(String.class);
            }

            @Test
            void whenContentStoreIsnTTheFirstExtendedInterfaceItStillGetTheDomainType() {
                Class<?> domainClass = AbstractStoreFactoryBean.getDomainClass(
                    ContentStoreNotFirstInterfaceStore.class);
                assertThat(domainClass).isEqualTo(String.class);
            }
        }

        @Nested
        class GetContentIdClass {
            @Test
            void getsTheDomainId() {
                Class<? extends Serializable> domainId = AbstractStoreFactoryBean.getContentIdClass(TestStore.class);
                assertThat(domainId).isEqualTo(UUID.class);
            }
        }
    }
}
