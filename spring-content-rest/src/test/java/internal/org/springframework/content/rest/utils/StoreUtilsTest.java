package internal.org.springframework.content.rest.utils;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.assertj.core.api.Assertions.assertThat;

import internal.org.springframework.content.commons.storeservice.StoreInfoImpl;
import internal.org.springframework.content.rest.support.TestEntity;
import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.Store;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.rest.StoreRestResource;

import java.util.UUID;

import static org.mockito.Mockito.mock;

@SuppressWarnings({ "deprecation", "rawtypes", "unchecked" })
public class StoreUtilsTest {

	private StoreInfo info;
	private String storePath;

	
    @Nested
    class StorePath {
        @Nested
        class GivenAContentStoreWithNoAnnotation {
            @BeforeEach
            void setUp() {
                ContentStore storeImpl = mock(TestContentStore.class);
                					info = new StoreInfoImpl(TestContentStore.class, TestEntity.class, storeImpl);

                storePath = StoreUtils.storePath(info);
            }
            @Test
            void shouldReturnReturnTestEntities() {
                assertThat(storePath).isEqualTo("testEntities");
            }
        }
        @Nested
        class GivenAContentStoreWithAStoreRestResourceAnnotation {
            @BeforeEach
            void setUp() {
                ContentStore storeImpl = mock(ContentStoreWithAnnotation.class);
                					info = new StoreInfoImpl(ContentStoreWithAnnotation.class,
                							TestEntity.class, storeImpl);

                storePath = StoreUtils.storePath(info);
            }
            @Test
            void shouldReturnReturnTheSpecifiedPath() {
                assertThat(storePath).isEqualTo("testEntities");
            }
        }
        @Nested
        class GivenAContentStoreWithAStoreRestResourceAnnotationThatSpecifiesAPath {
            @BeforeEach
            void setUp() {
                ContentStore storeImpl = mock(ContentStoreWithAnotherPath.class);
                					info = new StoreInfoImpl(ContentStoreWithAnotherPath.class, TestEntity.class, storeImpl);

                storePath = StoreUtils.storePath(info);
            }
            @Test
            void shouldReturnReturnTheSpecifiedPath() {
                assertThat(storePath).isEqualTo("some-other-path");
            }
        }
        @Nested
        class GivenAStoreWithNoAnnotations {
            @BeforeEach
            void setUp() {
                Store storeImpl = mock(TestStore.class);
                					info = new StoreInfoImpl(TestStore.class, null, storeImpl);

                storePath = StoreUtils.storePath(info);
            }
            @Test
            void shouldReturnTests() {
                assertThat(storePath).isEqualTo("tests");
            }
        }
        @Nested
        class GivenAStoreWithAStoreRestResourceAnnotation {
            @BeforeEach
            void setUp() {
                Store storeImpl = mock(TestStoreWithAnnotation.class);
                					info = new StoreInfoImpl(TestStoreWithAnnotation.class, null, storeImpl);

                storePath = StoreUtils.storePath(info);
            }
            @Test
            void shouldReturnTests() {
                assertThat(storePath).isEqualTo("testWithAnnotations");
            }
        }
        @Nested
        class GivenAStoreWithAStoreRestResourceAnnotationWithAPathOfFoo {
            @BeforeEach
            void setUp() {
                Store storeImpl = mock(TestStoreWithPath.class);
                					info = new StoreInfoImpl(TestStoreWithPath.class, null, storeImpl);

                storePath = StoreUtils.storePath(info);
            }
            @Test
            void shouldReturnTests() {
                assertThat(storePath).isEqualTo("foo");
            }
        }
    }


	public interface TestStore extends Store<String> {}

	@StoreRestResource
	public interface TestStoreWithAnnotation extends Store<String> {}

	@StoreRestResource(path="foo")
	public interface TestStoreWithPath extends Store<String> {}

	public interface TestContentStore extends ContentStore<TestEntity, UUID> {}

	@StoreRestResource
	public interface ContentStoreWithAnnotation extends ContentStore<TestEntity, UUID> {}

	@StoreRestResource(path = "some-other-path")
	public interface ContentStoreWithAnotherPath extends ContentStore<TestEntity, UUID> {}

	public interface StoreWithRenderable extends ContentStore<TestEntity, UUID>, Store<UUID>, Renderable<TestEntity> {}
}
