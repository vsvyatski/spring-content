package internal.org.springframework.content.jpa.store;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;

import internal.org.springframework.content.jpa.io.GenericBlobResource;
import jakarta.persistence.Id;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.jpa.io.BlobResource;
import org.springframework.content.jpa.io.BlobResourceLoader;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class DefaultJpaStoreImplTest {

    private DefaultJpaStoreImpl<Object, String> store;

    private BlobResourceLoader blobResourceLoader;

    private TestEntity entity;
    private JakartaTestEntity jakartaAnnotatedEntity;
    private InputStream stream;
    private InputStream inputStream;
    private Resource inputResource;
    private OutputStream outputStream;
    private Resource resource;
    private BlobResource blobResource;
    private String id;
    private Exception e;

    
    @Nested
    class DefaultJpaStoreImplCases {
        @Nested
        class Store {
            @Nested
            class GetResource {
                @Nested
                class GivenAnId {
                    @BeforeEach
                    void setUp() {
                        blobResourceLoader = mock(BlobResourceLoader.class);
                        id = "1";
                        store =
                                            spy(new DefaultJpaStoreImpl(blobResourceLoader, null, 8096));
                        resource = store.getResource(id);
                    }

                    @Test
                    void shouldUseTheBlobResourceLoaderToLoadABlobResource() {
                        verify(blobResourceLoader).getResource(id);
                    }

                }

            }

        }

        @Nested
        class AssociativeStore {
            @Nested
            class GetResource {
                @Nested
                class WhenTheEntityIsNotAssociatedWithAResource {
                    @BeforeEach
                    void setUp() {
                        blobResourceLoader = mock(BlobResourceLoader.class);
                        entity = new TestEntity();
                        store =
                                            spy(new DefaultJpaStoreImpl(blobResourceLoader, null, 8096));
                        resource = store.getResource(entity);
                    }

                    @Test
                    void shouldReturnNull() {
                        verify(blobResourceLoader, never()).getResource(any());
                        assertThat(resource).isNull();
                    }

                }

                @Nested
                class WhenTheEntityIsAssociatedWithAResource {
                    @BeforeEach
                    void setUp() {
                        blobResourceLoader = mock(BlobResourceLoader.class);
                        entity = new TestEntity();
                        entity.setContentId("12345");

                        store =
                                            spy(new DefaultJpaStoreImpl(blobResourceLoader, null, 8096));
                        resource = store.getResource(entity);
                    }

                    @Test
                    void shouldLoadANewResource() {
                        verify(blobResourceLoader).getResource(eq("12345"));
                    }

                }

            }

            @Nested
            class Associate {
                @BeforeEach
                void setUp() throws IOException {
                    blobResourceLoader = mock(BlobResourceLoader.class);
                    id = "12345";

                    entity = new TestEntity();

                    resource = mock(BlobResource.class);
                    when(blobResourceLoader.getResource(eq("12345")))
                            .thenReturn(resource);
                    when(resource.contentLength()).thenReturn(20L);

                    store =
                                        spy(new DefaultJpaStoreImpl(blobResourceLoader, null, 8096));
                    store.associate(entity, id);
                }

                @Test
                void shouldSetTheEntitySContentIDAttribute() {
                    assertThat(entity.getContentId()).isEqualTo("12345");
                }

            }

            @Nested
            class Unassociate {
                @BeforeEach
                void setUp() {
                    blobResourceLoader = mock(BlobResourceLoader.class);
                    id = "12345";

                    entity = new TestEntity();
                    entity.setContentId(id);
                    entity.setContentLen(20L);

                    store =
                                        spy(new DefaultJpaStoreImpl(blobResourceLoader, null, 8096));
                    store.unassociate(entity);
                }

                @Test
                void shouldResetTheContentId() {
                    assertThat(entity.getContentId()).isNull();
                }

            }

        }

        @Nested
        class ContentStore {
            @Nested
            class GetContent {
                @Nested
                class GivenContent {
                    @BeforeEach
                    void setUp() throws IOException {
                        blobResourceLoader = mock(BlobResourceLoader.class);
                        resource = mock(GenericBlobResource.class);

                        entity = new TestEntity("12345");

                        when(blobResourceLoader.getResource(entity.getContentId()))
                                .thenReturn(resource);

                        stream = new ByteArrayInputStream(
                                "hello content world!".getBytes());

                        when(resource.getInputStream()).thenReturn(stream);

                        store =
                                            spy(new DefaultJpaStoreImpl(blobResourceLoader, null, 8096));
                        try {
                            inputStream = store.getContent(entity);
                        } catch (Exception e) {
                            DefaultJpaStoreImplTest.this.e = e;
                        }
                    }

                    @Test
                    void shouldUseTheBlobResourceFactoryToCreateANewBlobResource() {
                        verify(blobResourceLoader).getResource(entity.getContentId());
                    }

                    @Test
                    void shouldReturnAnInputStream() {
                        assertThat(inputStream).isNotNull();
                    }

                }

                @Nested
                class GivenFetchingTheInputStreamFails {
                    @BeforeEach
                    void setUp() throws IOException {
                        blobResourceLoader = mock(BlobResourceLoader.class);
                        resource = mock(GenericBlobResource.class);

                        entity = new TestEntity("12345");

                        when(blobResourceLoader.getResource(entity.getContentId()))
                                .thenReturn(resource);

                        when(resource.getInputStream())
                                                        .thenThrow(new IOException("get-ioexception"));
                        store =
                                            spy(new DefaultJpaStoreImpl(blobResourceLoader, null, 8096));
                        try {
                            inputStream = store.getContent(entity);
                        } catch (Exception e) {
                            DefaultJpaStoreImplTest.this.e = e;
                        }
                    }

                    @Test
                    void shouldReturnNullAndThrowAStoreAccessException() {
                        assertThat(inputStream).isNull();
                        assertThat(e).isInstanceOf(StoreAccessException.class);
                        assertThat(e.getCause().getMessage()).isEqualTo("get-ioexception");
                    }

                }

            }

            @Nested
            class SetContent {
                @BeforeEach
                void setUp() throws IOException {
                    blobResourceLoader = mock(BlobResourceLoader.class);

                    entity = new TestEntity();
                    byte[] content = new byte[5000];
                    new Random().nextBytes(content);
                    inputStream = new ByteArrayInputStream(content);

                    resource = mock(BlobResource.class);
                    when(blobResourceLoader.getResource(matches(
                            "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}")))
                            .thenReturn(resource);
                    outputStream = mock(OutputStream.class);
                    when(((BlobResource) resource).getOutputStream())
                            .thenReturn(outputStream);
                    when(((BlobResource) resource).getId()).thenReturn(12345);

                    store =
                                        spy(new DefaultJpaStoreImpl(blobResourceLoader, null, 8096));
                    try {
                        store.setContent(entity, inputStream);
                    } catch (Exception e) {
                        DefaultJpaStoreImplTest.this.e = e;
                    }
                }

                @Test
                void shouldWriteTheContentsOfTheInputStreamToTheResourceSOutputStream() throws IOException {
                    verify(outputStream, atLeastOnce()).write(any(), anyInt(), anyInt());
                }

                @Test
                void shouldUpdateTheContentIdField() {
                    assertThat(entity.getContentId()).isEqualTo("12345");
                }

                @Test
                void shouldUpdateTheContentLengthField() {
                    assertThat(entity.getContentLen()).isEqualTo(5000L);
                }

                @Nested
                class WhenTheResourceOutputStreamThrowsAnIOException {
                    @BeforeEach
                    void setUp() throws IOException {
                        blobResourceLoader = mock(BlobResourceLoader.class);

                        entity = new TestEntity();
                        byte[] content = new byte[5000];
                        new Random().nextBytes(content);
                        inputStream = new ByteArrayInputStream(content);

                        resource = mock(BlobResource.class);
                        when(blobResourceLoader.getResource(matches(
                                "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}")))
                                .thenReturn(resource);
                        outputStream = mock(OutputStream.class);
                        when(((BlobResource) resource).getOutputStream())
                                .thenReturn(outputStream);
                        when(((BlobResource) resource).getId()).thenReturn(12345);

                        when(((BlobResource) resource).getOutputStream())
                                                        .thenThrow(new IOException("set-ioexception"));
                        store =
                                            spy(new DefaultJpaStoreImpl(blobResourceLoader, null, 8096));
                        try {
                            store.setContent(entity, inputStream);
                        } catch (Exception e) {
                            DefaultJpaStoreImplTest.this.e = e;
                        }
                    }

                    @Test
                    void shouldThrowAStoreAccessException() {
                        assertThat(e).isInstanceOf(StoreAccessException.class);
                        assertThat(e.getCause().getMessage()).isEqualTo("set-ioexception");
                    }

                }

            }

            @Nested
            class SetContentFromResource {
                @BeforeEach
                void setUp() {
                    entity = new TestEntity();
                    stream = new ByteArrayInputStream("Hello content world!".getBytes());
                    inputResource = new InputStreamResource(stream);

                    store =
                                        spy(new DefaultJpaStoreImpl(blobResourceLoader, null, 8096));
                    try {
                        store.setContent(entity, inputResource);
                    } catch (Exception e) {
                        DefaultJpaStoreImplTest.this.e = e;
                    }
                }

                @Test
                void shouldDelegate() {
                    verify(store).setContent(eq(entity), eq(stream));
                }

                @Nested
                class WhenTheResourceThrowsAnIOException {
                    @BeforeEach
                    void setUp() throws IOException {
                        entity = new TestEntity();
                        stream = new ByteArrayInputStream("Hello content world!".getBytes());
                        inputResource = new InputStreamResource(stream);

                        inputResource = mock(Resource.class);
                        when(inputResource.getInputStream()).thenThrow(new IOException("setContent badness"));

                        store =
                                            spy(new DefaultJpaStoreImpl(blobResourceLoader, null, 8096));
                        try {
                            store.setContent(entity, inputResource);
                        } catch (Exception e) {
                            DefaultJpaStoreImplTest.this.e = e;
                        }
                    }

                    @Test
                    void shouldThrowAStoreAccessException() {
                        assertThat(e).isInstanceOf(StoreAccessException.class);
                        assertThat(e.getCause().getMessage()).contains("setContent badness");
                    }

                }

            }

            @Nested
            class UnsetContent {
                @BeforeEach
                void setUp() {
                    blobResourceLoader = mock(BlobResourceLoader.class);
                    blobResource = mock(GenericBlobResource.class);

                    entity = new TestEntity("12345");

                    when(blobResourceLoader.getResource(entity.getContentId()))
                            .thenReturn(blobResource);

                    store =
                                        spy(new DefaultJpaStoreImpl(blobResourceLoader, null, 8096));
                    try {
                        store.unsetContent(entity);
                    } catch (Exception e) {
                        DefaultJpaStoreImplTest.this.e = e;
                    }
                }

                @Test
                void shouldDeleteTheContent() throws IOException {
                    verify(blobResource).delete();
                }

                @Nested
                class ResourceDeleteThrowsAnException {
                    @BeforeEach
                    void setUp() throws IOException {
                        blobResourceLoader = mock(BlobResourceLoader.class);
                        blobResource = mock(GenericBlobResource.class);

                        entity = new TestEntity("12345");

                        when(blobResourceLoader.getResource(entity.getContentId()))
                                .thenReturn(blobResource);

                        doThrow(new IOException("unset-ioexception")).when(blobResource).delete();
                        store =
                                            spy(new DefaultJpaStoreImpl(blobResourceLoader, null, 8096));
                        try {
                            store.unsetContent(entity);
                        } catch (Exception e) {
                            DefaultJpaStoreImplTest.this.e = e;
                        }
                    }

                    @Test
                    void shouldThrowAStoreAccessException() {
                        assertThat(e).isInstanceOf(StoreAccessException.class);
                        assertThat(e.getCause().getMessage()).isEqualTo("unset-ioexception");
                    }

                }

            }

        }

    }

    @Nested
    class DefaultJpaStoreImplJakartaAnnotatedEntity {
        @Nested
        class Store {
            @Nested
            class GetResource {
                @Nested
                class GivenAnId {
                    @BeforeEach
                    void setUp() {
                        blobResourceLoader = mock(BlobResourceLoader.class);
                        id = "1";
                        store =
                                            spy(new DefaultJpaStoreImpl(blobResourceLoader, null, 8096));
                        resource = store.getResource(id);
                    }

                    @Test
                    void shouldUseTheBlobResourceLoaderToLoadABlobResource() {
                        verify(blobResourceLoader).getResource(id);
                    }

                }

            }

        }

        @Nested
        class AssociativeStore {
            @Nested
            class UnassociateJakartaAnnotatedEntity {
                @BeforeEach
                void setUp() {
                    blobResourceLoader = mock(BlobResourceLoader.class);
                    id = "12345";

                    jakartaAnnotatedEntity = new JakartaTestEntity();
                    jakartaAnnotatedEntity.setContentId(id);
                    jakartaAnnotatedEntity.setContentLen(20L);

                    store =
                                        spy(new DefaultJpaStoreImpl(blobResourceLoader, null, 8096));
                    store.unassociate(jakartaAnnotatedEntity);
                }

                @Test
                void shouldNOTResetTheContentId() {
                    assertThat(jakartaAnnotatedEntity.getContentId()).isEqualTo(id);
                }

            }

        }

    }

    public static class TestEntity {
        @ContentId
        private String contentId;
        @ContentLength
        private long contentLen;

        public TestEntity() {
            this.contentId = null;
        }

        public TestEntity(String contentId) {
            this.contentId = contentId;
        }

        public String getContentId() {
            return this.contentId;
        }

        public void setContentId(String contentId) {
            this.contentId = contentId;
        }

        public long getContentLen() {
            return contentLen;
        }

        public void setContentLen(long contentLen) {
            this.contentLen = contentLen;
        }

    }

    public static class JakartaTestEntity {
        @Id
        @ContentId
        private String contentId;
        @ContentLength
        private long contentLen;

        public JakartaTestEntity() {
            this.contentId = null;
        }

        public JakartaTestEntity(String contentId) {
            this.contentId = contentId;
        }

        public String getContentId() {
            return this.contentId;
        }

        public void setContentId(String contentId) {
            this.contentId = contentId;
        }

        public long getContentLen() {
            return contentLen;
        }

        public void setContentLen(long contentLen) {
            this.contentLen = contentLen;
        }
    }
}
