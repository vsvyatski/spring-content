package internal.org.springframework.content.rest.utils;

import internal.org.springframework.content.commons.store.service.StoreInfoImpl;
import internal.org.springframework.content.rest.support.UuidSingleContentEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.Store;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.rest.StoreRestResource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@SuppressWarnings({"deprecation", "rawtypes", "unchecked"})
public class StoreUtilsTest {

    private StoreInfo info;
    private String storePath;

    public interface TestStore extends Store<String> {
    }

    @StoreRestResource
    public interface TestStoreWithAnnotation extends Store<String> {
    }

    @StoreRestResource(path = "foo")
    public interface TestStoreWithPath extends Store<String> {
    }

    public interface TestContentStore extends ContentStore<UuidSingleContentEntity, UUID> {
    }

    @StoreRestResource
    public interface ContentStoreWithAnnotation extends ContentStore<UuidSingleContentEntity, UUID> {
    }

    @StoreRestResource(path = "some-other-path")
    public interface ContentStoreWithAnotherPath extends ContentStore<UuidSingleContentEntity, UUID> {
    }

    @Nested
    class StorePath {
        @Nested
        class GivenAContentStoreWithNoAnnotation {
            @BeforeEach
            void setUp() {
                ContentStore storeImpl = mock(TestContentStore.class);
                info = new StoreInfoImpl(TestContentStore.class, UuidSingleContentEntity.class, storeImpl);

                storePath = StoreUtils.storePath(info);
            }

            @Test
            void shouldReturnReturnTestEntities() {
                assertThat(storePath).isEqualTo("uuidSingleContentEntities");
            }
        }

        @Nested
        class GivenAContentStoreWithAStoreRestResourceAnnotation {
            @BeforeEach
            void setUp() {
                ContentStore storeImpl = mock(ContentStoreWithAnnotation.class);
                info = new StoreInfoImpl(ContentStoreWithAnnotation.class,
                    UuidSingleContentEntity.class, storeImpl);

                storePath = StoreUtils.storePath(info);
            }

            @Test
            void shouldReturnReturnTheSpecifiedPath() {
                assertThat(storePath).isEqualTo("uuidSingleContentEntities");
            }
        }

        @Nested
        class GivenAContentStoreWithAStoreRestResourceAnnotationThatSpecifiesAPath {
            @BeforeEach
            void setUp() {
                ContentStore storeImpl = mock(ContentStoreWithAnotherPath.class);
                info = new StoreInfoImpl(ContentStoreWithAnotherPath.class, UuidSingleContentEntity.class, storeImpl);

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
}
