package org.springframework.content.commons.store.factory.stores;

import org.junit.jupiter.api.extension.ExtendWith;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.Serializable;
import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.store.AssociativeStore;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.Store;
import org.springframework.content.commons.store.factory.testsupport.EnableTestStores;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import internal.org.springframework.content.commons.repository.AnnotatedStoreEventInvoker;

@ContextConfiguration(classes = StoreTest.StoreTestConfiguration.class)
@ExtendWith(SpringExtension.class)
public class StoreTest {

	@Autowired
	private ApplicationContext context;

	
    @Nested
    class GivenAStoreDefinition {
        @Nested
        class GivenTheApplicationContext {
            @Test
            void shouldHaveAStoreBean() {
                assertThat(context.getBean(TestContentRepository.class)).isNotNull();

            }

            @Test
            void shouldHaveTheCoreSpringContentServiceBeans() {
                assertThat(context.getBean(AnnotatedStoreEventInvoker.class)).isNotNull();

            }

            @Test
            void shouldHaveATestStoreBean() {
                assertThat(context.getBean(TestStore.class)).isNotNull();

            }

            @Test
            void shouldHaveAnTestAssociativeStoreBean() {
                assertThat(context.getBean(TestAssociativeStore.class)).isNotNull();

            }

            @Test
            void shouldHaveAnTestAssociativeAndContentStoreBean() {
                assertThat(context.getBean(TestAssociativeAndContentStore.class)).isNotNull();

            }

        }

    }

	@Configuration
	@EnableTestStores
	public static class StoreTestConfiguration {
	}

	public interface TestContentRepository extends ContentStore<Object, Serializable> {
	}

	public interface TestStore extends Store<URI> {
	}

	public interface TestAssociativeStore extends AssociativeStore<Object, URI> {
	}

	public interface TestAssociativeAndContentStore extends ContentStore<Object, URI> {
	}

}
