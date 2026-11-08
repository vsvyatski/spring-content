package internal.org.springframework.content.fs.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.commons.utils.FileService;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.content.commons.utils.PlacementServiceImpl;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;

import java.io.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Disabled("This test was entirely commented out, let's keep it but not run it, for now.")
public class DefaultFileSystemStoresImplTest {
    private static final String CONTENT_ID = "abcd-efgh";

    private DefaultFileSystemStoreImpl<ContentProperty, String> filesystemContentRepoImpl;
    private FileSystemResourceLoader loader;
    private PlacementService placer;
    private ContentProperty entity;

    private Resource resource, inputResource;
    private WritableResource writeableResource;
    private DeletableResource deletableResource;
    private DeletableResource nonExistentResource;
    private FileService fileService;

    private InputStream content;
    private OutputStream output;

    private File parent;
    private File root;

    private String id;

    private InputStream result;
    private Exception e;

    @Nested
    class DefaultFileSystemContentRepositoryImpl {
        @Nested
        class Store {
            @Nested
            class GetResource {
                @BeforeEach
                void setUp() {
                    loader = mock(FileSystemResourceLoader.class);
                    placer = mock(PlacementService.class);
                    fileService = mock(FileService.class);

                    filesystemContentRepoImpl = spy(new DefaultFileSystemStoreImpl<>(
                            loader, null, placer, fileService));

                    id = "12345-67890";

                    when(placer.convert(eq("12345-67890"), eq(String.class)))
                            .thenReturn("12345-67890");

                    resource = filesystemContentRepoImpl.getResource(id);
                }

                @Test
                void shouldUseThePlacerServiceToGetAResourcePath() {
                    verify(placer).convert(eq("12345-67890"), eq(String.class));
                    verify(loader).getResource(eq("12345-67890"));
                }
            }
        }

        @Nested
        class AssociativeStore {
            @Nested
            class GetResource {
                @Nested
                class WhenTheEntityIsNotAlreadyAssociatedWithAResource {
                    @BeforeEach
                    void setUp() {
                        loader = mock(FileSystemResourceLoader.class);
                        placer = mock(PlacementService.class);
                        fileService = mock(FileService.class);

                        filesystemContentRepoImpl = spy(new DefaultFileSystemStoreImpl<>(
                                loader, null, placer, fileService));

                        entity = new TestEntity();
                        resource = filesystemContentRepoImpl.getResource(entity);
                    }

                    @Test
                    void shouldNotReturnAResource() {
                        assertThat(resource).isNull();
                    }
                }

                @Nested
                class WhenTheEntityIsAlreadyAssociatedWithAResource {
                    @BeforeEach
                    void setUp() {
                        loader = mock(FileSystemResourceLoader.class);
                        placer = mock(PlacementService.class);
                        fileService = mock(FileService.class);

                        filesystemContentRepoImpl = spy(new DefaultFileSystemStoreImpl<>(
                                loader, null, placer, fileService));

                        entity = new TestEntity();
                        entity.setContentId("12345-67890");

                        when(placer.convert(eq("12345-67890"),
                                eq(String.class))).thenReturn("/12345/67890");

                        resource = filesystemContentRepoImpl.getResource(entity);
                    }

                    @Test
                    void shouldUseThePlacerServiceToGetAResourcePath() {
                        verify(placer).convert(eq("12345-67890"), eq(String.class));
                        verify(loader).getResource(eq("/12345/67890"));
                    }
                }

                @Nested
                class WhenThereIsAnEntityConverter {
                    @BeforeEach
                    void setUp() {
                        loader = mock(FileSystemResourceLoader.class);
                        placer = mock(PlacementService.class);
                        fileService = mock(FileService.class);

                        filesystemContentRepoImpl = spy(new DefaultFileSystemStoreImpl<>(
                                loader, null, placer, fileService));

                        entity = new TestEntity();

                        deletableResource = mock(DeletableResource.class);

                        when(placer.canConvert(eq(entity.getClass()), eq(String.class))).thenReturn(true);
                        when(placer.convert(eq(entity), eq(String.class))).thenReturn("/abcd/efgh");
                        when(loader.getResource("/abcd/efgh")).thenReturn(deletableResource);

                        resource = filesystemContentRepoImpl.getResource(entity);
                    }

                    @Test
                    void shouldNotNeedToConvertTheId() {
                        verify(placer, never()).convert(not(entity), eq(String.class));
                    }

                    @Test
                    void shouldReturnTheResource() {
                        assertThat(resource).isEqualTo(deletableResource);
                    }
                }

                @Nested
                class WhenTheEntityHasAStringArgConstructorIssue57 {
                    @BeforeEach
                    void setUp() {
                        loader = mock(FileSystemResourceLoader.class);
                        placer = mock(PlacementService.class);
                        fileService = mock(FileService.class);

                        filesystemContentRepoImpl = spy(new DefaultFileSystemStoreImpl<>(
                                loader, null, placer, fileService));

                        PlacementService placementService = new PlacementServiceImpl();
                        placer = spy(placementService);

                        entity = new TestEntity();

                        resource = filesystemContentRepoImpl.getResource(entity);
                    }

                    @Test
                    void shouldNotCallThePlacementServiceTryingToConvertTheEntityToAString() {
                        verify(placer, never()).convert(eq(entity), eq(String.class));
                    }
                }
            }

            @Nested
            class Associate {
                @BeforeEach
                void setUp() throws IOException {
                    loader = mock(FileSystemResourceLoader.class);
                    placer = mock(PlacementService.class);
                    fileService = mock(FileService.class);

                    filesystemContentRepoImpl = spy(new DefaultFileSystemStoreImpl<>(
                            loader, null, placer, fileService));

                    id = "12345-67890";

                    entity = new TestEntity();

                    when(placer.convert(eq("12345-67890"), eq(String.class))).thenReturn("/12345/67890");

                    deletableResource = mock(DeletableResource.class);
                    when(loader.getResource(eq("/12345/67890")))
                            .thenReturn(deletableResource);

                    when(deletableResource.contentLength()).thenReturn(20L);

                    filesystemContentRepoImpl.associate(entity, id);
                }

                @Test
                void shouldSetTheEntitySContentIDAttribute() {
                    assertThat(entity.getContentId()).isEqualTo("12345-67890");
                }
            }

            @Nested
            class Unassociate {
                @BeforeEach
                void setUp() {
                    loader = mock(FileSystemResourceLoader.class);
                    placer = mock(PlacementService.class);
                    fileService = mock(FileService.class);

                    filesystemContentRepoImpl = spy(new DefaultFileSystemStoreImpl<>(
                            loader, null, placer, fileService));

                    entity = new TestEntity();
                    entity.setContentId("12345-67890");

                    filesystemContentRepoImpl.unassociate(entity);
                }

                @Test
                void shouldResetTheEntitySContentIDAttribute() {
                    assertThat(entity.getContentId()).isNull();
                }
            }
        }

        @Nested
        class ContentStore {
            @Nested
            class SetContent {
                @Nested
                class GivenAnEntityConverter {
                    @Nested
                    class WhenTheContentDoesntYetExist {
                        @BeforeEach
                        void setUp() throws IOException {
                            loader = mock(FileSystemResourceLoader.class);
                            placer = mock(PlacementService.class);
                            fileService = mock(FileService.class);

                            filesystemContentRepoImpl = spy(new DefaultFileSystemStoreImpl<>(
                                    loader, null, placer, fileService));

                            writeableResource = mock(WritableResource.class);
                            entity = new TestEntity();
                            content = new ByteArrayInputStream(
                                    "Hello content world!".getBytes());

                            when(placer.convert(matches(
                                            "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"),
                                    eq(String.class)))
                                    .thenReturn("12345-67890");

                            when(loader.getResource(eq("12345-67890")))
                                    .thenReturn(writeableResource);
                            output = mock(OutputStream.class);
                            when(writeableResource.getOutputStream()).thenReturn(output);

                            File resourceFile = mock(File.class);
                            parent = mock(File.class);
                            when(writeableResource.getFile()).thenReturn(resourceFile);
                            when(resourceFile.getParentFile()).thenReturn(parent);

                            try {
                                filesystemContentRepoImpl.setContent(entity, content);
                            } catch (Exception e) {
                                DefaultFileSystemStoresImplTest.this.e = e;
                            }
                        }

                        @Test
                        void createsADirectoryForTheParent() throws IOException {
                            verify(fileService).mkdirs(eq(parent));
                        }

                        @Test
                        void shouldMakeANewUUID() {
                            assertThat(entity.getContentId()).isNotNull();
                        }

                        @Test
                        void shouldCreateANewResource() {
                            verify(loader).getResource(eq("12345-67890"));
                        }

                        @Test
                        void shouldWriteToTheResourceSOutputStream() throws IOException {
                            verify(writeableResource).getOutputStream();
                            verify(output, times(1)).write(ArgumentMatchers.any(), eq(0),
                                    eq(20));
                        }
                    }

                    @Nested
                    class WhenTheContentAlreadyExists {
                        @BeforeEach
                        void setUp() throws IOException {
                            loader = mock(FileSystemResourceLoader.class);
                            placer = mock(PlacementService.class);
                            fileService = mock(FileService.class);

                            filesystemContentRepoImpl = spy(new DefaultFileSystemStoreImpl<>(
                                    loader, null, placer, fileService));

                            writeableResource = mock(WritableResource.class);
                            entity = new TestEntity();
                            content = new ByteArrayInputStream(
                                    "Hello content world!".getBytes());

                            when(placer.canConvert(eq(entity.getClass()), eq(String.class))).thenReturn(true);
                            when(placer.convert(eq(entity), eq(String.class))).thenReturn("/abcd/efgh");
                            when(loader.getResource(eq("/abcd/efgh"))).thenReturn(writeableResource);

                            when(writeableResource.exists()).thenReturn(true);

                            output = mock(OutputStream.class);
                            when(writeableResource.getOutputStream()).thenReturn(output);

                            when(writeableResource.contentLength()).thenReturn(20L);

                            try {
                                filesystemContentRepoImpl.setContent(entity, content);
                            } catch (Exception e) {
                                DefaultFileSystemStoresImplTest.this.e = e;
                            }
                        }

                        @Test
                        void shouldWriteToTheResourceSOutputStream() throws IOException {
                            verify(output, times(1)).write(ArgumentMatchers.any(), eq(0),
                                    eq(20));
                            verify(output).close();
                        }

                        @Test
                        void shouldChangeTheContentLength() {
                            assertThat(entity.getContentLen()).isEqualTo(20L);
                        }

                    }

                }

                @Nested
                class GivenJustTheDefaultIDConverters {
                    @Nested
                    class WhenTheContentAlreadyExists {
                        @BeforeEach
                        void setUp() throws IOException {
                            loader = mock(FileSystemResourceLoader.class);
                            placer = mock(PlacementService.class);
                            fileService = mock(FileService.class);

                            filesystemContentRepoImpl = spy(new DefaultFileSystemStoreImpl<>(
                                    loader, null, placer, fileService));

                            writeableResource = mock(WritableResource.class);
                            entity = new TestEntity();
                            content = new ByteArrayInputStream(
                                    "Hello content world!".getBytes());

                            when(placer.convert(eq(entity), eq(String.class))).thenReturn(null);

                            when(placer.convert(eq("12345-67890"),
                                    eq(String.class)))
                                    .thenReturn("12345-67890");

                            when(placer.convert(matches(
                                            "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"),
                                    eq(String.class)))
                                    .thenReturn("12345-67890");

                            when(loader.getResource(eq("12345-67890")))
                                    .thenReturn(writeableResource);
                            output = mock(OutputStream.class);
                            when(writeableResource.getOutputStream()).thenReturn(output);

                            when(writeableResource.contentLength()).thenReturn(20L);

                            entity.setContentId("12345-67890");
                            when(writeableResource.exists()).thenReturn(true);

                            try {
                                filesystemContentRepoImpl.setContent(entity, content);
                            } catch (Exception e) {
                                DefaultFileSystemStoresImplTest.this.e = e;
                            }
                        }

                        @Test
                        void shouldUseThePlacerServiceToGetAResourcePath() {
                            verify(placer, atLeastOnce()).convert(any(), Object.class);
                            verify(loader).getResource(eq("12345-67890"));
                        }

                        @Test
                        void shouldChangeTheContentLength() {
                            assertThat(entity.getContentLen()).isEqualTo(20L);
                        }

                        @Test
                        void shouldWriteToTheResourceSOutputStream() throws IOException {
                            verify(writeableResource).getOutputStream();
                            verify(output, times(1)).write(ArgumentMatchers.any(), eq(0),
                                    eq(20));
                        }
                    }

                    @Nested
                    class WhenTheContentDoesNotAlreadyExist {
                        @BeforeEach
                        void setUp() throws IOException {
                            loader = mock(FileSystemResourceLoader.class);
                            placer = mock(PlacementService.class);
                            fileService = mock(FileService.class);

                            filesystemContentRepoImpl = spy(new DefaultFileSystemStoreImpl<>(
                                    loader, null, placer, fileService));

                            writeableResource = mock(WritableResource.class);
                            entity = new TestEntity();
                            content = new ByteArrayInputStream(
                                    "Hello content world!".getBytes());

                            when(placer.convert(eq(entity), eq(String.class))).thenReturn(null);

                            when(placer.convert(eq("12345-67890"),
                                    eq(String.class)))
                                    .thenReturn("12345-67890");

                            when(placer.convert(matches(
                                            "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"),
                                    eq(String.class)))
                                    .thenReturn("12345-67890");

                            when(loader.getResource(eq("12345-67890")))
                                    .thenReturn(writeableResource);
                            output = mock(OutputStream.class);
                            when(writeableResource.getOutputStream()).thenReturn(output);

                            when(writeableResource.contentLength()).thenReturn(20L);

                            assertThat(entity.getContentId()).isNull();

                            File resourceFile = mock(File.class);
                            parent = mock(File.class);

                            when(writeableResource.getFile()).thenReturn(resourceFile);
                            when(resourceFile.getParentFile()).thenReturn(parent);

                            try {
                                filesystemContentRepoImpl.setContent(entity, content);
                            } catch (Exception e) {
                                DefaultFileSystemStoresImplTest.this.e = e;
                            }
                        }

                        @Test
                        void createsADirectoryForTheParent() throws IOException {
                            verify(fileService).mkdirs(eq(parent));
                        }

                        @Test
                        void shouldMakeANewUUID() {
                            assertThat(entity.getContentId()).isNotNull();
                        }

                        @Test
                        void shouldCreateANewResource() {
                            verify(loader).getResource(eq("12345-67890"));
                        }

                        @Test
                        void shouldWriteToTheResourceSOutputStream() throws IOException {
                            verify(writeableResource).getOutputStream();
                            verify(output, times(1)).write(ArgumentMatchers.any(), eq(0),
                                    eq(20));
                        }
                    }

                    @Nested
                    class WhenGettingTheResourceOutputStreamThrowsAnIOException {
                        @BeforeEach
                        void setUp() throws IOException {
                            loader = mock(FileSystemResourceLoader.class);
                            placer = mock(PlacementService.class);
                            fileService = mock(FileService.class);

                            filesystemContentRepoImpl = spy(new DefaultFileSystemStoreImpl<>(
                                    loader, null, placer, fileService));

                            writeableResource = mock(WritableResource.class);
                            entity = new TestEntity();
                            content = new ByteArrayInputStream(
                                    "Hello content world!".getBytes());

                            when(placer.convert(eq(entity), eq(String.class))).thenReturn(null);

                            when(placer.convert(eq("12345-67890"),
                                    eq(String.class)))
                                    .thenReturn("12345-67890");

                            when(placer.convert(matches(
                                            "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"),
                                    eq(String.class)))
                                    .thenReturn("12345-67890");

                            when(loader.getResource(eq("12345-67890")))
                                    .thenReturn(writeableResource);
                            output = mock(OutputStream.class);
                            when(writeableResource.getOutputStream()).thenReturn(output);

                            when(writeableResource.contentLength()).thenReturn(20L);

                            File resourceFile = mock(File.class);
                            parent = mock(File.class);

                            when(writeableResource.getFile()).thenReturn(resourceFile);
                            when(resourceFile.getParentFile()).thenReturn(parent);

                            when(writeableResource.getOutputStream()).thenThrow(new IOException());

                            try {
                                filesystemContentRepoImpl.setContent(entity, content);
                            } catch (Exception e) {
                                DefaultFileSystemStoresImplTest.this.e = e;
                            }
                        }

                        @Test
                        void shouldReturnAStoreAccessExceptionWrappingTheIOException() {
                            assertThat(e).isInstanceOf(StoreAccessException.class);
                            assertThat(e.getCause()).isInstanceOf(IOException.class);
                        }
                    }
                }
            }

            @Nested
            class SetContentFromResource {
                @BeforeEach
                void setUp() {
                    loader = mock(FileSystemResourceLoader.class);
                    placer = mock(PlacementService.class);
                    fileService = mock(FileService.class);

                    filesystemContentRepoImpl = spy(new DefaultFileSystemStoreImpl<>(
                            loader, null, placer, fileService));

                    writeableResource = mock(WritableResource.class);
                    entity = new TestEntity();
                    content = new ByteArrayInputStream("Hello content world!".getBytes());
                    inputResource = new InputStreamResource(content);

                    try {
                        filesystemContentRepoImpl.setContent(entity, inputResource);
                    } catch (Exception e) {
                        DefaultFileSystemStoresImplTest.this.e = e;
                    }
                }

                @Test
                void shouldDelegateToSetContentFromInputStream() {
                    verify(filesystemContentRepoImpl).setContent(eq(entity), eq(content));
                }

                @Nested
                class WhenTheResourceThrowsAnIOException {
                    @BeforeEach
                    void setUp() throws IOException {
                        loader = mock(FileSystemResourceLoader.class);
                        placer = mock(PlacementService.class);
                        fileService = mock(FileService.class);

                        filesystemContentRepoImpl = spy(new DefaultFileSystemStoreImpl<>(
                                loader, null, placer, fileService));

                        writeableResource = mock(WritableResource.class);
                        entity = new TestEntity();
                        content = new ByteArrayInputStream("Hello content world!".getBytes());
                        inputResource = new InputStreamResource(content);

                        inputResource = mock(Resource.class);
                        when(inputResource.getInputStream()).thenThrow(new IOException("setContent badness"));

                        try {
                            filesystemContentRepoImpl.setContent(entity, inputResource);
                        } catch (Exception e) {
                            DefaultFileSystemStoresImplTest.this.e = e;
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
            class GetContent {
                @Nested
                class GivenAnEntityConverter {
                    @Nested
                    class WhenTheResourceDoesNotExists {
                        @BeforeEach
                        void setUp() throws IOException {
                            loader = mock(FileSystemResourceLoader.class);
                            placer = mock(PlacementService.class);
                            fileService = mock(FileService.class);

                            filesystemContentRepoImpl = spy(new DefaultFileSystemStoreImpl<>(
                                    loader, null, placer, fileService));

                            writeableResource = mock(WritableResource.class);
                            entity = new TestEntity();
                            content = mock(InputStream.class);
                            entity.setContentId(CONTENT_ID);

                            when(placer.convert(eq(entity), eq(String.class)))
                                    .thenReturn(null);

                            when(placer.convert(eq(CONTENT_ID), eq(String.class)))
                                    .thenReturn(CONTENT_ID);

                            when(loader.getResource(eq(CONTENT_ID)))
                                    .thenReturn(writeableResource);
                            when(writeableResource.getInputStream()).thenReturn(content);

                            when(placer.canConvert(eq(entity.getClass()), eq(String.class))).thenReturn(true);
                            when(placer.convert(eq(entity), eq(String.class)))
                                    .thenReturn("/abcd/efgh");

                            nonExistentResource = mock(DeletableResource.class);

                            when(loader.getResource(eq("/abcd/efgh")))
                                    .thenReturn(nonExistentResource);

                            when(writeableResource.exists()).thenReturn(false);

                            try {
                                result = filesystemContentRepoImpl.getContent(entity);
                            } catch (Exception e) {
                                DefaultFileSystemStoresImplTest.this.e = e;
                            }
                        }

                        @Test
                        void shouldNotReturnTheContent() {
                            assertThat(result).isNull();
                        }
                    }

                    @Nested
                    class WhenTheResourceExists {
                        @BeforeEach
                        void setUp() throws IOException {
                            loader = mock(FileSystemResourceLoader.class);
                            placer = mock(PlacementService.class);
                            fileService = mock(FileService.class);

                            filesystemContentRepoImpl = spy(new DefaultFileSystemStoreImpl<>(
                                    loader, null, placer, fileService));

                            writeableResource = mock(WritableResource.class);
                            entity = new TestEntity();
                            content = mock(InputStream.class);
                            entity.setContentId(CONTENT_ID);

                            when(placer.convert(eq(entity), eq(String.class)))
                                    .thenReturn(null);

                            when(placer.convert(eq(CONTENT_ID), eq(String.class)))
                                    .thenReturn(CONTENT_ID);

                            when(loader.getResource(eq(CONTENT_ID)))
                                    .thenReturn(writeableResource);
                            when(writeableResource.getInputStream()).thenReturn(content);

                            when(placer.canConvert(eq(entity.getClass()), eq(String.class))).thenReturn(true);
                            when(placer.convert(eq(entity), eq(String.class)))
                                    .thenReturn("/abcd/efgh");

                            when(loader.getResource(eq("/abcd/efgh")))
                                    .thenReturn(writeableResource);

                            when(writeableResource.exists()).thenReturn(true);

                            when(writeableResource.getInputStream()).thenReturn(content);

                            try {
                                result = filesystemContentRepoImpl.getContent(entity);
                            } catch (Exception e) {
                                DefaultFileSystemStoresImplTest.this.e = e;
                            }
                        }

                        @Test
                        void shouldGetContent() {
                            assertThat(result).isEqualTo(content);
                        }
                    }
                }

                @Nested
                class GivenJustTheDefaultIDConverter {
                    @Nested
                    class WhenTheResourceDoesNotExists {
                        @BeforeEach
                        void setUp() throws IOException {
                            loader = mock(FileSystemResourceLoader.class);
                            placer = mock(PlacementService.class);
                            fileService = mock(FileService.class);

                            filesystemContentRepoImpl = spy(new DefaultFileSystemStoreImpl<>(
                                    loader, null, placer, fileService));

                            writeableResource = mock(WritableResource.class);
                            entity = new TestEntity();
                            content = mock(InputStream.class);
                            entity.setContentId(CONTENT_ID);

                            when(placer.convert(eq(entity), eq(String.class))).thenReturn(null);

                            when(placer.convert(eq(CONTENT_ID), eq(String.class)))
                                    .thenReturn(CONTENT_ID);

                            when(loader.getResource(eq(CONTENT_ID))).thenReturn(writeableResource);
                            when(writeableResource.getInputStream()).thenReturn(content);

                            when(placer.convert(eq(entity), eq(String.class))).thenReturn(null);
                            nonExistentResource = mock(DeletableResource.class);
                            when(writeableResource.exists()).thenReturn(true);

                            when(loader.getResource(eq("/abcd/efgh"))).thenReturn(nonExistentResource);
                            when(loader.getResource(eq(CONTENT_ID))).thenReturn(nonExistentResource);

                            try {
                                result = filesystemContentRepoImpl.getContent(entity);
                            } catch (Exception e) {
                                DefaultFileSystemStoresImplTest.this.e = e;
                            }
                        }

                        @Test
                        void shouldNotFindTheContent() {
                            assertThat(result).isNull();
                        }
                    }

                    @Nested
                    class WhenTheResourceExists {
                        @BeforeEach
                        void setUp() throws IOException {
                            loader = mock(FileSystemResourceLoader.class);
                            placer = mock(PlacementService.class);
                            fileService = mock(FileService.class);

                            filesystemContentRepoImpl = spy(new DefaultFileSystemStoreImpl<>(
                                    loader, null, placer, fileService));

                            writeableResource = mock(WritableResource.class);
                            entity = new TestEntity();
                            content = mock(InputStream.class);
                            entity.setContentId(CONTENT_ID);

                            when(placer.convert(eq(entity), eq(String.class)))
                                    .thenReturn(null);

                            when(placer.convert(eq(CONTENT_ID), eq(String.class)))
                                    .thenReturn(CONTENT_ID);

                            when(loader.getResource(eq(CONTENT_ID)))
                                    .thenReturn(writeableResource);
                            when(writeableResource.getInputStream()).thenReturn(content);

                            when(placer.convert(eq(entity), eq(String.class))).thenReturn(null);
                            when(writeableResource.exists()).thenReturn(true);
                            try {
                                result = filesystemContentRepoImpl.getContent(entity);
                            } catch (Exception e) {
                                DefaultFileSystemStoresImplTest.this.e = e;
                            }
                        }

                        @Test
                        void shouldGetContent() {
                            assertThat(result).isEqualTo(content);
                        }

                        @Nested
                        class WhenGettingTheResourceInputStreamThrowsAnIOException {
                            @BeforeEach
                            void setUp() throws IOException {
                                loader = mock(FileSystemResourceLoader.class);
                                placer = mock(PlacementService.class);
                                fileService = mock(FileService.class);

                                filesystemContentRepoImpl = spy(new DefaultFileSystemStoreImpl<>(
                                        loader, null, placer, fileService));

                                writeableResource = mock(WritableResource.class);
                                entity = new TestEntity();
                                content = mock(InputStream.class);
                                entity.setContentId(CONTENT_ID);

                                when(placer.convert(eq(entity), eq(String.class)))
                                        .thenReturn(null);

                                when(placer.convert(eq(CONTENT_ID), eq(String.class)))
                                        .thenReturn(CONTENT_ID);

                                when(loader.getResource(eq(CONTENT_ID)))
                                        .thenReturn(writeableResource);
                                when(writeableResource.getInputStream()).thenReturn(content);

                                when(placer.convert(eq(entity), eq(String.class))).thenReturn(null);
                                when(writeableResource.exists()).thenReturn(true);
                                when(writeableResource.getInputStream()).thenThrow(new IOException("test-ioexception"));
                                try {
                                    result = filesystemContentRepoImpl.getContent(entity);
                                } catch (Exception e) {
                                    DefaultFileSystemStoresImplTest.this.e = e;
                                }
                            }

                            @Test
                            void shouldReturnAStoreAccessExceptionWrappingTheIOException() {
                                assertThat(result).isNull();
                                assertThat(e).isInstanceOf(StoreAccessException.class);
                                assertThat(e.getCause().getMessage()).isEqualTo("test-ioexception");
                            }
                        }
                    }

                    @Nested
                    class WhenTheResourceExistsButInTheOldLocation {
                        @BeforeEach
                        void setUp() throws IOException {
                            loader = mock(FileSystemResourceLoader.class);
                            placer = mock(PlacementService.class);
                            fileService = mock(FileService.class);

                            filesystemContentRepoImpl = spy(new DefaultFileSystemStoreImpl<>(
                                    loader, null, placer, fileService));

                            writeableResource = mock(WritableResource.class);
                            entity = new TestEntity();
                            content = mock(InputStream.class);
                            entity.setContentId(CONTENT_ID);

                            when(placer.convert(eq(entity), eq(String.class)))
                                    .thenReturn(null);

                            when(placer.convert(eq(CONTENT_ID), eq(String.class)))
                                    .thenReturn(CONTENT_ID);

                            when(loader.getResource(eq(CONTENT_ID)))
                                    .thenReturn(writeableResource);
                            when(writeableResource.getInputStream()).thenReturn(content);

                            when(placer.convert(eq(entity), eq(String.class))).thenReturn(null);
                            nonExistentResource = mock(DeletableResource.class);
                            when(loader.getResource(eq("/abcd/efgh")))
                                    .thenReturn(nonExistentResource);
                            when(nonExistentResource.exists()).thenReturn(false);

                            when(loader.getResource(eq(CONTENT_ID)))
                                    .thenReturn(writeableResource);
                            when(writeableResource.exists()).thenReturn(true);

                            try {
                                result = filesystemContentRepoImpl.getContent(entity);
                            } catch (Exception e) {
                                DefaultFileSystemStoresImplTest.this.e = e;
                            }
                        }

                        @Test
                        void shouldCheckTheNewLocationAndThenTheOld() {
                            InOrder inOrder = Mockito.inOrder(loader);

                            inOrder.verify(loader).getResource(eq(CONTENT_ID));
                            inOrder.verifyNoMoreInteractions();
                        }

                        @Test
                        void shouldGetContent() {
                            assertThat(result).isEqualTo(content);
                        }
                    }
                }
            }

            @Nested
            class UnsetContent {
                @Nested
                class GivenAnEntityConverter {
                    @Nested
                    class GivenTheResourceDoesNotExist {
                        @BeforeEach
                        void setUp() {
                            loader = mock(FileSystemResourceLoader.class);
                            placer = mock(PlacementService.class);
                            fileService = mock(FileService.class);

                            filesystemContentRepoImpl = spy(new DefaultFileSystemStoreImpl<>(
                                    loader, null, placer, fileService));

                            writeableResource = mock(WritableResource.class);
                            entity = new TestEntity();
                            entity.setContentId(CONTENT_ID);
                            entity.setContentLen(100L);
                            deletableResource = mock(DeletableResource.class);

                            when(placer.canConvert(eq(entity.getClass()), eq(String.class))).thenReturn(true);
                            when(placer.convert(eq(entity), eq(String.class)))
                                    .thenReturn("/abcd/efgh");

                            nonExistentResource = mock(DeletableResource.class);

                            when(loader.getResource(eq("/abcd/efgh")))
                                    .thenReturn(nonExistentResource);

                            when(nonExistentResource.exists()).thenReturn(false);

                            filesystemContentRepoImpl.unsetContent(entity);
                        }

                        @Test
                        void shouldNotDeleteTheResource() throws IOException {
                            verify(nonExistentResource, never()).delete();
                        }
                    }

                    @Nested
                    class GivenTheResourceExists {
                        @BeforeEach
                        void setUp() throws IOException {
                            loader = mock(FileSystemResourceLoader.class);
                            placer = mock(PlacementService.class);
                            fileService = mock(FileService.class);

                            filesystemContentRepoImpl = spy(new DefaultFileSystemStoreImpl<>(
                                    loader, null, placer, fileService));

                            writeableResource = mock(WritableResource.class);
                            entity = new TestEntity();
                            entity.setContentId(CONTENT_ID);
                            entity.setContentLen(100L);
                            deletableResource = mock(DeletableResource.class);

                            when(placer.canConvert(eq(entity.getClass()), eq(String.class))).thenReturn(true);
                            when(placer.convert(eq(entity), eq(String.class)))
                                    .thenReturn("/abcd/efgh");

                            deletableResource = mock(DeletableResource.class);

                            when(loader.getResource(eq("/abcd/efgh")))
                                    .thenReturn(deletableResource);

                            File resourceFile = mock(File.class);
                            parent = mock(File.class);
                            when(deletableResource.getFile()).thenReturn(resourceFile);
                            when(resourceFile.getParentFile()).thenReturn(parent);
                            when(deletableResource.exists()).thenReturn(true);

                            FileSystemResource rootResource = mock(FileSystemResource.class);
                            when(loader.getRootResource()).thenReturn(rootResource);
                            root = mock(File.class);
                            when(rootResource.getFile()).thenReturn(root);

                            filesystemContentRepoImpl.unsetContent(entity);
                        }

                        @Test
                        void shouldDeleteTheResource() throws IOException {
                            verify(deletableResource, times(1)).delete();
                        }
                    }
                }

                @Nested
                class GivenJustTheDefaultIDConverter {
                    @Nested
                    class WhenTheContentExistsInTheNewLocation {
                        @BeforeEach
                        void setUp() throws IOException {
                            loader = mock(FileSystemResourceLoader.class);
                            placer = mock(PlacementService.class);
                            fileService = mock(FileService.class);

                            filesystemContentRepoImpl = spy(new DefaultFileSystemStoreImpl<>(
                                    loader, null, placer, fileService));

                            writeableResource = mock(WritableResource.class);
                            entity = new TestEntity();
                            entity.setContentId(CONTENT_ID);
                            entity.setContentLen(100L);
                            deletableResource = mock(DeletableResource.class);

                            when(placer.convert(eq(entity), eq(String.class))).thenReturn(null);
                            when(placer.convert(eq(CONTENT_ID), eq(String.class)))
                                    .thenReturn(CONTENT_ID);

                            when(loader.getResource(eq(CONTENT_ID)))
                                    .thenReturn(deletableResource);

                            File resourceFile = mock(File.class);
                            parent = mock(File.class);
                            when(deletableResource.getFile()).thenReturn(resourceFile);
                            when(resourceFile.getParentFile()).thenReturn(parent);
                            when(deletableResource.exists()).thenReturn(true);

                            FileSystemResource rootResource = mock(FileSystemResource.class);
                            when(loader.getRootResource()).thenReturn(rootResource);
                            root = mock(File.class);
                            when(rootResource.getFile()).thenReturn(root);

                            filesystemContentRepoImpl.unsetContent(entity);
                        }

                        @Test
                        void shouldDeleteTheResource() throws IOException {
                            verify(deletableResource, times(1)).delete();
                        }

                        @Nested
                        class WhenThePropertyHasADedicatedContentIdField {
                            @BeforeEach
                            void setUp() throws IOException {
                                loader = mock(FileSystemResourceLoader.class);
                                placer = mock(PlacementService.class);
                                fileService = mock(FileService.class);

                                filesystemContentRepoImpl = spy(new DefaultFileSystemStoreImpl<>(
                                        loader, null, placer, fileService));

                                writeableResource = mock(WritableResource.class);
                                entity = new TestEntity();
                                entity.setContentId(CONTENT_ID);
                                entity.setContentLen(100L);
                                deletableResource = mock(DeletableResource.class);

                                when(placer.convert(eq(entity), eq(String.class))).thenReturn(null);
                                when(placer.convert(eq(CONTENT_ID), eq(String.class)))
                                        .thenReturn(CONTENT_ID);

                                when(loader.getResource(eq(CONTENT_ID)))
                                        .thenReturn(deletableResource);

                                File resourceFile = mock(File.class);
                                parent = mock(File.class);
                                when(deletableResource.getFile()).thenReturn(resourceFile);
                                when(resourceFile.getParentFile()).thenReturn(parent);
                                when(deletableResource.exists()).thenReturn(true);

                                FileSystemResource rootResource = mock(FileSystemResource.class);
                                when(loader.getRootResource()).thenReturn(rootResource);
                                root = mock(File.class);
                                when(rootResource.getFile()).thenReturn(root);

                                filesystemContentRepoImpl.unsetContent(entity);
                            }

                            @Test
                            void shouldResetTheMetadata() {
                                assertThat(entity.getContentId()).isNull();
                                assertThat(entity.getContentLen()).isEqualTo(0L);
                            }
                        }

                        @Nested
                        class WhenThePropertySContentIdFieldAlsoIsTheJavaxPersistenceIdField {
                            @BeforeEach
                            void setUp() throws IOException {
                                loader = mock(FileSystemResourceLoader.class);
                                placer = mock(PlacementService.class);
                                fileService = mock(FileService.class);

                                filesystemContentRepoImpl = spy(new DefaultFileSystemStoreImpl<>(
                                        loader, null, placer, fileService));

                                writeableResource = mock(WritableResource.class);
                                entity = new TestEntity();
                                entity.setContentId(CONTENT_ID);
                                entity.setContentLen(100L);
                                deletableResource = mock(DeletableResource.class);

                                when(placer.convert(eq(entity), eq(String.class))).thenReturn(null);
                                when(placer.convert(eq(CONTENT_ID), eq(String.class)))
                                        .thenReturn(CONTENT_ID);

                                when(loader.getResource(eq(CONTENT_ID)))
                                        .thenReturn(deletableResource);

                                File resourceFile = mock(File.class);
                                parent = mock(File.class);
                                when(deletableResource.getFile()).thenReturn(resourceFile);
                                when(resourceFile.getParentFile()).thenReturn(parent);
                                when(deletableResource.exists()).thenReturn(true);

                                FileSystemResource rootResource = mock(FileSystemResource.class);
                                when(loader.getRootResource()).thenReturn(rootResource);
                                root = mock(File.class);
                                when(rootResource.getFile()).thenReturn(root);

                                entity = new SharedIdContentIdEntity();
                                entity.setContentId(CONTENT_ID);

                                filesystemContentRepoImpl.unsetContent(entity);
                            }

                            @Test
                            void shouldNotResetTheContentIdMetadata() {
                                assertThat(entity.getContentId()).isEqualTo(CONTENT_ID);
                                assertThat(entity.getContentLen()).isEqualTo(0L);
                            }
                        }

                        @Nested
                        class WhenThePropertySContentIdFieldAlsoIsTheSpringIdField {
                            @BeforeEach
                            void setUp() throws IOException {
                                loader = mock(FileSystemResourceLoader.class);
                                placer = mock(PlacementService.class);
                                fileService = mock(FileService.class);

                                filesystemContentRepoImpl = spy(new DefaultFileSystemStoreImpl<>(
                                        loader, null, placer, fileService));

                                writeableResource = mock(WritableResource.class);
                                entity = new TestEntity();
                                entity.setContentId(CONTENT_ID);
                                entity.setContentLen(100L);
                                deletableResource = mock(DeletableResource.class);

                                when(placer.convert(eq(entity), eq(String.class))).thenReturn(null);
                                when(placer.convert(eq(CONTENT_ID), eq(String.class)))
                                        .thenReturn(CONTENT_ID);

                                when(loader.getResource(eq(CONTENT_ID)))
                                        .thenReturn(deletableResource);

                                File resourceFile = mock(File.class);
                                parent = mock(File.class);
                                when(deletableResource.getFile()).thenReturn(resourceFile);
                                when(resourceFile.getParentFile()).thenReturn(parent);
                                when(deletableResource.exists()).thenReturn(true);

                                FileSystemResource rootResource = mock(FileSystemResource.class);
                                when(loader.getRootResource()).thenReturn(rootResource);
                                root = mock(File.class);
                                when(rootResource.getFile()).thenReturn(root);

                                entity = new SharedSpringIdContentIdEntity();
                                entity.setContentId(CONTENT_ID);

                                filesystemContentRepoImpl.unsetContent(entity);
                            }

                            @Test
                            void shouldNotResetTheContentIdMetadata() {
                                assertThat(entity.getContentId()).isEqualTo(CONTENT_ID);
                                assertThat(entity.getContentLen()).isEqualTo(0L);
                            }
                        }
                    }

                    @Nested
                    class WhenTheContentDoesntExist {
                        @BeforeEach
                        void setUp() {
                            loader = mock(FileSystemResourceLoader.class);
                            placer = mock(PlacementService.class);
                            fileService = mock(FileService.class);

                            filesystemContentRepoImpl = spy(new DefaultFileSystemStoreImpl<>(
                                    loader, null, placer, fileService));

                            writeableResource = mock(WritableResource.class);
                            entity = new TestEntity();
                            entity.setContentId(CONTENT_ID);
                            entity.setContentLen(100L);
                            deletableResource = mock(DeletableResource.class);

                            when(placer.convert(eq(entity), eq(String.class))).thenReturn(null);
                            when(placer.convert(eq(CONTENT_ID), eq(String.class)))
                                    .thenReturn(CONTENT_ID);

                            nonExistentResource = mock(DeletableResource.class);
                            when(loader.getResource(eq(CONTENT_ID)))
                                    .thenReturn(nonExistentResource);
                            when(nonExistentResource.exists()).thenReturn(false);

                            filesystemContentRepoImpl.unsetContent(entity);
                        }

                        @Test
                        void shouldUnsetTheContent() throws IOException {
                            verify(nonExistentResource, never()).delete();
                            assertThat(entity.getContentId()).isNull();
                            assertThat(entity.getContentLen()).isEqualTo(0L);
                        }
                    }
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
            this.contentId = contentId;
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

    public static class SharedSpringIdContentIdEntity implements ContentProperty {

        @org.springframework.data.annotation.Id
        @ContentId
        private String contentId;

        @ContentLength
        private long contentLen;

        public SharedSpringIdContentIdEntity() {
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
