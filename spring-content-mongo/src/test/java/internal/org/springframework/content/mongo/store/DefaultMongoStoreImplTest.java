package internal.org.springframework.content.mongo.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultMongoStoreImplTest {

    private DefaultMongoStoreImpl<Object, String> mongoContentRepoImpl;
    private GridFsTemplate gridFsTemplate;
    private ContentProperty property;
    private GridFsResource resource;
    private Resource inputResource;
    private PlacementService placer;

    private InputStream content;
    private InputStream result;
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
        class WhenTheEntityHasAStringArgConstructorIssue57 {
            @BeforeEach
            void entity() {
                mongoContentRepoImpl = new DefaultMongoStoreImpl<>(gridFsTemplate, null, placer);
                property = new TestEntity();
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
                property = new TestEntity();
                property.setContentId("12345");
            }

            @Nested
            class WhenTheEntityHasASharedIdContentIdField {
                @BeforeEach
                void sharedId() {
                    property = new SharedIdContentIdEntity();
                    property.setContentId("12345");
                    mongoContentRepoImpl.unassociate(property);
                }

                @Test
                void shouldNotResetTheContentIdBecauseItIsAlsoTheId() {
                    assertThat(property.getContentId()).isEqualTo("12345");
                }
            }
        }
    }

    @Nested
    class ContentStore {
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
                property = new TestEntity();
                content = mock(InputStream.class);
            }

            @Nested
            class WhenTheContentAlreadyExists {
                @BeforeEach
                void existing() throws Exception {
                    property.setContentId("abcd-efghi");
                    when(placer.convert(eq("abcd-efghi"), eq(String.class))).thenReturn("abcd-efghi");
                    when(gridFsTemplate.getResource("abcd-efghi")).thenReturn(resource);
                    when(resource.exists()).thenReturn(true);
                    when(resource.contentLength()).thenReturn(1L);
                }

                @Nested
                class WhenTheGridfsStoreThrowsAnException {
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
                property = new TestEntity();
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
            class WhenTheResourceThrowsAnIOException {
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
                property = new TestEntity();
                property.setContentId("abcd");
                content = mock(InputStream.class);
                when(placer.convert(eq("abcd"), eq(String.class))).thenReturn("abcd");
                when(gridFsTemplate.getResource("abcd")).thenReturn(resource);
                when(resource.getInputStream()).thenReturn(content);
            }

            @Nested
            class WhenTheResourceExists {
                @BeforeEach
                void exists() {
                    when(resource.exists()).thenReturn(true);
                }

                @Nested
                class WhenTheResourceInputStreamThrowsAnIOException {
                    @BeforeEach
                    void brokenStream() throws IOException {
                        when(resource.getInputStream()).thenThrow(new IOException("get-ioexception"));
                        try {
                            result = mongoContentRepoImpl.getContent(property);
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
                property = new TestEntity();
                property.setContentId("abcd");
                when(placer.convert(eq("abcd"), eq(String.class))).thenReturn("abcd");
                when(gridFsTemplate.getResource("abcd")).thenReturn(resource);
                when(resource.exists()).thenReturn(true);
            }

            @Nested
            class WhenGridfsDeletionThrowsAnException {
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

    public interface ContentProperty {
        String getContentId();

        void setContentId(String contentId);

        long getContentLen();

        void setContentLen(long contentLen);
    }

    public static class TestEntity implements ContentProperty {

        @ContentId
        private String contentId;

        @ContentLength
        private long contentLen;

        public TestEntity() {
            this.contentId = null;
        }

        public TestEntity(String contentId) {
            this.contentId = new String(contentId);
        }

        @Override
        public String getContentId() {
            return this.contentId;
        }

        @Override
        public void setContentId(String contentId) {
            this.contentId = contentId;
        }

        @Override
        public long getContentLen() {
            return contentLen;
        }

        @Override
        public void setContentLen(long contentLen) {
            this.contentLen = contentLen;
        }
    }

    public static class SharedIdContentIdEntity implements ContentProperty {

        @jakarta.persistence.Id
        @ContentId
        private String contentId;

        @ContentLength
        private long contentLen;

        public SharedIdContentIdEntity() {
            this.contentId = null;
        }

        @Override
        public String getContentId() {
            return this.contentId;
        }

        @Override
        public void setContentId(String contentId) {
            this.contentId = contentId;
        }

        @Override
        public long getContentLen() {
            return contentLen;
        }

        @Override
        public void setContentLen(long contentLen) {
            this.contentLen = contentLen;
        }
    }
}
