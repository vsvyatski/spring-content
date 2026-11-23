package internal.org.springframework.content.mongo.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.content.test.support.ContentAccessor;
import org.springframework.content.test.support.SingleContentFields;
import org.springframework.content.test.support.mongo.SharedIdJakartaMongoDocument;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.content.test.support.TestSupportData.SAMPLE_CONTENT_ID;
import static org.springframework.content.test.support.TestSupportData.SAMPLE_SHORT_CONTENT_ID;

public class DefaultMongoStoreImplTest {

    private DefaultMongoStoreImpl<Object, String> mongoContentRepoImpl;
    private GridFsTemplate gridFsTemplate;
    private ContentAccessor<String> property;
    private GridFsResource resource;
    private Resource inputResource;
    private PlacementService placer;

    private InputStream content;
    private Exception e;

    @Nested
   class AssociativeStore {
        @BeforeEach
        void setUp() {
            placer = mock(PlacementService.class);
            gridFsTemplate = mock(GridFsTemplate.class);
            resource = mock(GridFsResource.class);
            mongoContentRepoImpl = new DefaultMongoStoreImpl<>(gridFsTemplate, null, placer);
        }

        @Nested

        @DisplayName("When the entity has a string arg constructor issue57")

        class EntityHasAStringArgConstructorIssue57 {
            @BeforeEach
            void entity() {
                mongoContentRepoImpl = new DefaultMongoStoreImpl<>(gridFsTemplate, null, placer);
                property = new SingleContentFields();
            }

            @Test
            void shouldNotCallThePlacementServiceByDefault() {
                verify(placer, never()).convert(property, String.class);
            }
        }

        @Nested
        class Unassociate {
            @BeforeEach
            void entity() {
                property = new SingleContentFields();
                property.setContentId(SAMPLE_SHORT_CONTENT_ID);
            }

            @Nested

            @DisplayName("When the entity has a shared id content ID field")

            class EntityHasASharedIdContentIdField {
                @BeforeEach
                void sharedId() {
                    property = new SharedIdJakartaMongoDocument();
                    property.setContentId(SAMPLE_SHORT_CONTENT_ID);
                    mongoContentRepoImpl.unassociate(property);
                }

                @Test
                void shouldNotResetTheContentIdBecauseItIsAlsoTheId() {
                    assertThat(property.getContentId()).isEqualTo(SAMPLE_SHORT_CONTENT_ID);
                }
            }
        }
    }

    @Nested
   class ContentStoreScenario {
        @BeforeEach
        void setUp() {
            placer = mock(PlacementService.class);
            gridFsTemplate = mock(GridFsTemplate.class);
            resource = mock(GridFsResource.class);
            mongoContentRepoImpl = spy(new DefaultMongoStoreImpl<>(gridFsTemplate, null, placer));
        }

        @Nested
        class SetContent {
            @BeforeEach
            void entity() {
                property = new SingleContentFields();
                content = mock(InputStream.class);
            }

            @Nested

            @DisplayName("When the content already exists")

            class ContentAlreadyExists {
                @BeforeEach
                void existing() throws Exception {
                    property.setContentId(SAMPLE_CONTENT_ID);
                    when(placer.convert(eq(SAMPLE_CONTENT_ID), eq(String.class))).thenReturn(SAMPLE_CONTENT_ID);
                    when(gridFsTemplate.getResource(SAMPLE_CONTENT_ID)).thenReturn(resource);
                    when(resource.exists()).thenReturn(true);
                    when(resource.contentLength()).thenReturn(1L);
                }

                @Nested

                @DisplayName("When the gridfs store throws an exception")

                class GridfsStoreThrowsAnException {
                    @BeforeEach
                    void storeFails() {
                        when(gridFsTemplate.store(any(), anyString())).thenThrow(new RuntimeException("set-exception"));
                        try {
                            mongoContentRepoImpl.setContent(property, content);
                        } catch (Exception ex) {
                            e = ex;
                        }
                    }

                    @Test
                    void shouldThrowAStoreAccessException() {
                        assertThat(e).isInstanceOf(StoreAccessException.class);
                        assertThat(e.getCause().getMessage()).isEqualTo("set-exception");
                    }
                }
            }
        }

        @Nested
        class SetContentFromResource {
            @BeforeEach
            void entity() {
                property = new SingleContentFields();
                content = new ByteArrayInputStream("Hello content world!".getBytes());
                inputResource = new InputStreamResource(content);
            }

            @Test
            void shouldDelegate() {
                try {
                    mongoContentRepoImpl.setContent(property, inputResource);
                } catch (RuntimeException ignored) {
                    // the delegated setContent(InputStream) still runs before placement fails
                }
                verify(mongoContentRepoImpl).setContent(eq(property), eq(content));
            }

            @Nested

            @DisplayName("When the resource throws an io exception")

            class ResourceThrowsAnIOException {
                @BeforeEach
                void brokenResource() throws IOException {
                    inputResource = mock(Resource.class);
                    when(inputResource.getInputStream()).thenThrow(new IOException("setContent badness"));
                    try {
                        mongoContentRepoImpl.setContent(property, inputResource);
                    } catch (Exception ex) {
                        e = ex;
                    }
                }

                @Test
                void shouldThrowAStoreAccessException() {
                    assertThat(e).isInstanceOf(StoreAccessException.class);
                    assertThat(e.getCause()).hasMessageContaining("setContent badness");
                }
            }
        }

        @Nested
        class GetContent {
            @BeforeEach
            void entity() throws IOException {
                property = new SingleContentFields();
                property.setContentId(SAMPLE_SHORT_CONTENT_ID);
                content = mock(InputStream.class);
                when(placer.convert(eq(SAMPLE_SHORT_CONTENT_ID), eq(String.class))).thenReturn(SAMPLE_SHORT_CONTENT_ID);
                when(gridFsTemplate.getResource(SAMPLE_SHORT_CONTENT_ID)).thenReturn(resource);
                when(resource.getInputStream()).thenReturn(content);
            }

            @Nested

            @DisplayName("When the resource exists")

            class ResourceExists {
                @BeforeEach
                void exists() {
                    when(resource.exists()).thenReturn(true);
                }

                @Nested

                @DisplayName("When the resource input stream throws an io exception")

                class ResourceInputStreamThrowsAnIOException {
                    @BeforeEach
                    void brokenStream() throws IOException {
                        when(resource.getInputStream()).thenThrow(new IOException("get-ioexception"));
                        try {
                            mongoContentRepoImpl.getContent(property);
                        } catch (Exception ex) {
                            e = ex;
                        }
                    }

                    @Test
                    void shouldThrowAStoreAccessException() {
                        assertThat(e).isInstanceOf(StoreAccessException.class);
                        assertThat(e.getCause().getMessage()).isEqualTo("get-ioexception");
                    }
                }
            }
        }

        @Nested
        class UnsetContent {
            @BeforeEach
            void entity() {
                property = new SingleContentFields();
                property.setContentId(SAMPLE_SHORT_CONTENT_ID);
                when(placer.convert(eq(SAMPLE_SHORT_CONTENT_ID), eq(String.class))).thenReturn(SAMPLE_SHORT_CONTENT_ID);
                when(gridFsTemplate.getResource(SAMPLE_SHORT_CONTENT_ID)).thenReturn(resource);
                when(resource.exists()).thenReturn(true);
            }

            @Nested

            @DisplayName("When gridfs deletion throws an exception")

            class GridfsDeletionThrowsAnException {
                @BeforeEach
                void deleteFails() {
                    doThrow(new RuntimeException("unset-exception")).when(gridFsTemplate).delete(any());
                    try {
                        mongoContentRepoImpl.unsetContent(property);
                    } catch (Exception ex) {
                        e = ex;
                    }
                }

                @Test
                void shouldThrowAStoreAccessException() {
                    assertThat(e).isInstanceOf(StoreAccessException.class);
                    assertThat(e.getCause().getMessage()).isEqualTo("unset-exception");
                }
            }
        }
    }
}
