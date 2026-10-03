package org.springframework.content.commons.store.factory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.io.Serializable;
import java.util.UUID;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.*;
import org.springframework.core.io.Resource;

public class AbstractStoreFactoryBeanTest {

	
    @Nested
    class AbstractContentStoreFactoryBean {
        @Nested
        class GetDomainClass {
            @Test
            void getsTheDomainClass() {
                TestContentStoreFactory factory = new TestContentStoreFactory(TestStore.class);
                Class<?> domainClass = factory.getDomainClass(TestStore.class);
                assertThat(domainClass).isEqualTo(String.class);

            }

            @Test
            void whenContentStoreIsnTTheFirstExtendedInterfaceItStillGetTheDomainType() {
                TestContentStoreFactory factory = new TestContentStoreFactory(TestStore.class);
                Class<?> domainClass = factory.getDomainClass(
                		ContentStoreNotFirstIntefaceStore.class);
                assertThat(domainClass).isEqualTo(String.class);

            }

        }

        @Nested
        class GetContentIdClass {
            @Test
            void getsTheDomainId() {
                TestContentStoreFactory factory = new TestContentStoreFactory(TestStore.class);
                Class<? extends Serializable> domainId = factory
                		.getContentIdClass(TestStore.class);
                assertThat(domainId).isEqualTo(UUID.class);

            }

        }

    }

	public static class TestContentStoreFactory extends org.springframework.content.commons.store.factory.AbstractStoreFactoryBean {
		protected TestContentStoreFactory(Class<? extends Store> storeInterface) {
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
		public Object setContent(Object property, InputStream content) {
			return null;
		}

		@Override
		public Object setContent(Object property, Resource resourceContent) {
			return null;
		}

		@Override
		public Object unsetContent(Object property) {
			return null;
		}

		@Override
		public InputStream getContent(Object property) {
			return null;
		}

		@Override
		public Resource getResource(Object entity) {
			return null;
		}

		@Override
		public void associate(Object entity, Serializable id) {

		}

		@Override
		public void unassociate(Object entity) {

		}

		@Override
		public Resource getResource(Serializable id) {
			return null;
		}

        @Override
        public Resource getResource(Object entity, PropertyPath propertyPath) {
            // TODO Auto-generated method stub
            return null;
        }

		@Override
		public Resource getResource(Object entity, PropertyPath propertyPath, GetResourceParams params) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
        public void associate(Object entity, PropertyPath propertyPath, Serializable id) {
            // TODO Auto-generated method stub

        }

        @Override
        public void unassociate(Object entity, PropertyPath propertyPath) {
            // TODO Auto-generated method stub

        }

        @Override
        public Object setContent(Object property, PropertyPath propertyPath, InputStream content) {
            // TODO Auto-generated method stub
            return null;
        }

		public Object setContent(Object property, PropertyPath propertyPath, InputStream content, long contentLen) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object setContent(Object entity, PropertyPath propertyPath, InputStream content, SetContentParams params) {
			return null;
		}

		@Override
        public Object setContent(Object property, PropertyPath propertyPath, Resource resourceContent) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Object unsetContent(Object property, PropertyPath propertyPath) {
            // TODO Auto-generated method stub
            return null;
        }

		@Override
		public Object unsetContent(Object entity, PropertyPath propertyPath, UnsetContentParams params) {
			return null;
		}

		@Override
        public InputStream getContent(Object property, PropertyPath propertyPath) {
            // TODO Auto-generated method stub
            return null;
        }
	}

	public interface TestStore extends Serializable, ContentStore<String, UUID> {
	}

	public interface ContentStoreNotFirstIntefaceStore
			extends Serializable, ContentStore<String, UUID> {
	}
}
