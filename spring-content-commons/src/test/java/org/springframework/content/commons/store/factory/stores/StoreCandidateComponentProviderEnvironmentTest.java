package org.springframework.content.commons.store.factory.stores;

import org.junit.jupiter.api.extension.ExtendWith;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.store.AssociativeStore;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.Store;
import org.springframework.content.commons.store.factory.testsupport.EnableTestStores;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@ActiveProfiles(profiles = "c")
@ContextConfiguration(classes = StoreCandidateComponentProviderEnvironmentTest.StoreTestConfiguration.class)
@ExtendWith(SpringExtension.class)
public class StoreCandidateComponentProviderEnvironmentTest {

	@Autowired(required=false)
	private TestStore store;

	@Autowired(required=false)
	private TestAssociativeStore associativeStore;

	@Autowired(required=false)
	private TestContentStore contentStore;

	
    @Nested
    class GivenTwoStoresWithProfiles {
        @Test
        void shouldHaveAStoreBean() {
            assertThat(store).isNotNull();
            assertThat(associativeStore).isNull();
            assertThat(contentStore).isNotNull();

        }

    }

	@Configuration
	@EnableTestStores
	public static class StoreTestConfiguration {
	}

	public interface TestStore extends Store<URI> {
	}

	@Profile("b")
	public interface TestAssociativeStore extends AssociativeStore<Object, URI> {
	}

	@Profile("c")
	public interface TestContentStore extends ContentStore<Object, URI> {
	}

}
