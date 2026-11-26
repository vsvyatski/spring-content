package org.springframework.content.commons.store.factory;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
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

    public static class TestContentStoreFactory extends AbstractStoreFactoryBean {
        protected TestContentStoreFactory(Class<? extends Store<Serializable>> storeInterface) {
            super(storeInterface);
        }

        @Override
        protected Object getContentStoreImpl() {
            return new TestConfigStoreImpl();
        }
    }

    @NullMarked
    public static class TestConfigStoreImpl implements ContentStore<Object, Serializable> {

        @Override
        public Object setContent(Object property, InputStream content) {
            return property;
        }

        @Override
        public Object setContent(Object property, Resource resourceContent) {
            return property;
        }

        @Override
        public Object unsetContent(Object property) {
            return property;
        }

        @Override
        public @Nullable InputStream getContent(Object property) {
            return null;
        }

        @Override
        public @Nullable Resource getResource(Object entity) {
            return null;
        }

        @Override
        public void associate(Object entity, Serializable id) {
        }

        @Override
        public void unassociate(Object entity) {
        }

        @Override
        public @Nullable Resource getResource(Serializable id) {
            return null;
        }

        @Override
        public @Nullable Resource getResource(Object entity, PropertyPath propertyPath) {
            return null;
        }

        @Override
        public @Nullable Resource getResource(Object entity, PropertyPath propertyPath, GetResourceParams params) {
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
            return property;
        }

        @Override
        public Object setContent(Object property, PropertyPath propertyPath, InputStream content, long contentLen) {
            return property;
        }

        @Override
        public Object setContent(Object entity, PropertyPath propertyPath, InputStream content, SetContentParams params) {
            return entity;
        }

        @Override
        public Object setContent(Object property, PropertyPath propertyPath, Resource resourceContent) {
            return property;
        }

        @Override
        public Object unsetContent(Object property, PropertyPath propertyPath) {
            return property;
        }

        @Override
        public Object unsetContent(Object entity, PropertyPath propertyPath, UnsetContentParams params) {
            return entity;
        }

        @Override
        public @Nullable InputStream getContent(Object property, PropertyPath propertyPath) {
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
