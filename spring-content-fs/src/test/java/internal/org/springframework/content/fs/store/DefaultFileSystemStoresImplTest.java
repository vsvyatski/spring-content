package internal.org.springframework.content.fs.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.commons.utils.FileService;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.content.commons.utils.PlacementServiceImpl;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.test.support.ContentAccessor;
import org.springframework.content.test.support.SingleContentFields;
import org.springframework.content.test.support.jpa.SharedIdJakartaJpaEntity;
import org.springframework.content.test.support.jpa.SharedIdSpringJpaEntity;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;

import java.io.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.content.test.support.TestSupportData.SAMPLE_CONTENT_ID;
import static org.springframework.content.test.support.TestSupportData.SAMPLE_FS_RESOURCE_PATH;

public class DefaultFileSystemStoresImplTest {

    private DefaultFileSystemStoreImpl<ContentAccessor<String>, String> filesystemStoreImpl;
    private FileSystemResourceLoader loader;
    private PlacementService placer;
    private ContentAccessor<String> entity;

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
   class DefaultFileSystemStoresImplBehaviour {
        @Nested
        class Store {
            @Nested
            class GetResource {
                @BeforeEach
                void setUp() {
                    loader = mock(FileSystemResourceLoader.class);
                    placer = mock(PlacementService.class);
                    fileService = mock(FileService.class);

                    filesystemStoreImpl = spy(new DefaultFileSystemStoreImpl<>(
                        loader, null, placer, fileService));

                    id = SAMPLE_CONTENT_ID;

                    when(placer.convert(eq(SAMPLE_CONTENT_ID), eq(String.class)))
                        .thenReturn(SAMPLE_CONTENT_ID);

                    resource = filesystemStoreImpl.getResource(id);
                }

                @Test
                void shouldUseThePlacerServiceToGetAResourcePath() {
                    verify(placer).convert(eq(SAMPLE_CONTENT_ID), eq(String.class));
                    verify(loader).getResource(eq(SAMPLE_CONTENT_ID));
                }
            }
        }

        @Nested
        class AssociativeStore {
            @Nested
            class GetResource {
                @Nested
                @DisplayName("When the entity is not already associated with a resource")
                class EntityIsNotAlreadyAssociatedWithResource {
                    @BeforeEach
                    void setUp() {
                        loader = mock(FileSystemResourceLoader.class);
                        placer = mock(PlacementService.class);
                        fileService = mock(FileService.class);

                        filesystemStoreImpl = spy(new DefaultFileSystemStoreImpl<>(
                            loader, null, placer, fileService));

                        entity = new SingleContentFields();
                        resource = filesystemStoreImpl.getResource(entity);
                    }

                    @Test
                    void shouldNotReturnAResource() {
                        assertThat(resource).isNull();
                    }
                }

                @Nested

                @DisplayName("When the entity is already associated with a resource")

                class EntityIsAlreadyAssociatedWithResource {
                    @BeforeEach
                    void setUp() {
                        loader = mock(FileSystemResourceLoader.class);
                        placer = mock(PlacementService.class);
                        fileService = mock(FileService.class);

                        filesystemStoreImpl = spy(new DefaultFileSystemStoreImpl<>(
                            loader, null, placer, fileService));

                        entity = new SingleContentFields();
                        entity.setContentId(SAMPLE_CONTENT_ID);

                        when(placer.convert(eq(SAMPLE_CONTENT_ID),
                            eq(String.class))).thenReturn(SAMPLE_FS_RESOURCE_PATH);

                        resource = filesystemStoreImpl.getResource(entity);
                    }

                    @Test
                    void shouldUseThePlacerServiceToGetAResourcePath() {
                        verify(placer).convert(eq(SAMPLE_CONTENT_ID), eq(String.class));
                        verify(loader).getResource(eq(SAMPLE_FS_RESOURCE_PATH));
                    }
                }

                @Nested

                @DisplayName("When there is an entity converter")

                class EntityConverter {
                    @BeforeEach
                    void setUp() {
                        loader = mock(FileSystemResourceLoader.class);
                        placer = mock(PlacementService.class);
                        fileService = mock(FileService.class);

                        filesystemStoreImpl = spy(new DefaultFileSystemStoreImpl<>(
                            loader, null, placer, fileService));

                        entity = new SingleContentFields();

                        deletableResource = mock(DeletableResource.class);

                        when(placer.canConvert(eq(entity.getClass()), eq(String.class))).thenReturn(true);
                        when(placer.convert(eq(entity), eq(String.class))).thenReturn(SAMPLE_FS_RESOURCE_PATH);
                        when(loader.getResource(SAMPLE_FS_RESOURCE_PATH)).thenReturn(deletableResource);

                        resource = filesystemStoreImpl.getResource(entity);
                    }

                    @Test
                    void shouldNotNeedToConvertTheId() {
                        verify(placer, never()).convert(not(eq(entity)), eq(String.class));
                    }

                    @Test
                    void shouldReturnTheResource() {
                        assertThat(resource).isEqualTo(deletableResource);
                    }
                }

                @Nested

                @DisplayName("When the entity has a string-arg constructor")

                class EntityHasStringArgConstructor {
                    @BeforeEach
                    void setUp() {
                        loader = mock(FileSystemResourceLoader.class);
                        placer = mock(PlacementService.class);
                        fileService = mock(FileService.class);

                        filesystemStoreImpl = spy(new DefaultFileSystemStoreImpl<>(
                            loader, null, placer, fileService));

                        PlacementService placementService = new PlacementServiceImpl();
                        placer = spy(placementService);

                        entity = new SingleContentFields();

                        resource = filesystemStoreImpl.getResource(entity);
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

                    filesystemStoreImpl = spy(new DefaultFileSystemStoreImpl<>(
                        loader, null, placer, fileService));

                    id = SAMPLE_CONTENT_ID;

                    entity = new SingleContentFields();

                    when(placer.convert(eq(SAMPLE_CONTENT_ID), eq(String.class))).thenReturn(SAMPLE_FS_RESOURCE_PATH);

                    deletableResource = mock(DeletableResource.class);
                    when(loader.getResource(eq(SAMPLE_FS_RESOURCE_PATH)))
                        .thenReturn(deletableResource);

                    when(deletableResource.contentLength()).thenReturn(20L);

                    filesystemStoreImpl.associate(entity, id);
                }

                @Test
                void shouldSetTheEntitySContentIDAttribute() {
                    assertThat(entity.getContentId()).isEqualTo(SAMPLE_CONTENT_ID);
                }
            }

            @Nested
            class Unassociate {
                @BeforeEach
                void setUp() {
                    loader = mock(FileSystemResourceLoader.class);
                    placer = mock(PlacementService.class);
                    fileService = mock(FileService.class);

                    filesystemStoreImpl = spy(new DefaultFileSystemStoreImpl<>(
                        loader, null, placer, fileService));

                    entity = new SingleContentFields();
                    entity.setContentId(SAMPLE_CONTENT_ID);

                    filesystemStoreImpl.unassociate(entity);
                }

                @Test
                void shouldResetTheEntitySContentIDAttribute() {
                    assertThat(entity.getContentId()).isNull();
                }
            }
        }

        @Nested
        class ContentStoreScenario {
            @Nested
            class SetContent {
                @Nested
                @DisplayName("Given an entity converter")
                class EntityConverter {
                    @Nested
                    @DisplayName("When the content doesnt yet exist")
                    class ContentDoesntYetExist {
                        @BeforeEach
                        void setUp() throws IOException {
                            loader = mock(FileSystemResourceLoader.class);
                            placer = mock(PlacementService.class);
                            fileService = mock(FileService.class);

                            filesystemStoreImpl = spy(new DefaultFileSystemStoreImpl<>(
                                loader, null, placer, fileService));

                            writeableResource = mock(WritableResource.class);
                            entity = new SingleContentFields();
                            content = new ByteArrayInputStream(
                                "Hello content world!".getBytes());

                            when(placer.convert(matches(
                                    "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"),
                                eq(String.class)))
                                .thenReturn(SAMPLE_CONTENT_ID);

                            when(loader.getResource(eq(SAMPLE_CONTENT_ID)))
                                .thenReturn(writeableResource);
                            output = mock(OutputStream.class);
                            when(writeableResource.getOutputStream()).thenReturn(output);

                            File resourceFile = mock(File.class);
                            parent = mock(File.class);
                            when(writeableResource.getFile()).thenReturn(resourceFile);
                            when(resourceFile.getParentFile()).thenReturn(parent);

                            try {
                                filesystemStoreImpl.setContent(entity, content);
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
                            verify(loader).getResource(eq(SAMPLE_CONTENT_ID));
                        }

                        @Test
                        void shouldWriteToTheResourceSOutputStream() throws IOException {
                            verify(writeableResource).getOutputStream();
                            verify(output, times(1)).write(ArgumentMatchers.any(), eq(0),
                                eq(20));
                        }
                    }

                    @Nested

                    @DisplayName("When the content already exists")

                    class ContentAlreadyExists {
                        @BeforeEach
                        void setUp() throws IOException {
                            loader = mock(FileSystemResourceLoader.class);
                            placer = mock(PlacementService.class);
                            fileService = mock(FileService.class);

                            filesystemStoreImpl = spy(new DefaultFileSystemStoreImpl<>(
                                loader, null, placer, fileService));

                            writeableResource = mock(WritableResource.class);
                            entity = new SingleContentFields();
                            content = new ByteArrayInputStream(
                                "Hello content world!".getBytes());

                            when(placer.canConvert(eq(entity.getClass()), eq(String.class))).thenReturn(true);
                            when(placer.convert(eq(entity), eq(String.class))).thenReturn(SAMPLE_FS_RESOURCE_PATH);
                            when(loader.getResource(eq(SAMPLE_FS_RESOURCE_PATH))).thenReturn(writeableResource);

                            when(writeableResource.exists()).thenReturn(true);

                            output = mock(OutputStream.class);
                            when(writeableResource.getOutputStream()).thenReturn(output);

                            when(writeableResource.contentLength()).thenReturn(20L);

                            try {
                                filesystemStoreImpl.setContent(entity, content);
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

                @DisplayName("Given just the default id converters")

                class JustDefaultIDConverters {
                    @Nested
                    @DisplayName("When the content already exists")
                    class ContentAlreadyExists {
                        @BeforeEach
                        void setUp() throws IOException {
                            loader = mock(FileSystemResourceLoader.class);
                            placer = mock(PlacementService.class);
                            fileService = mock(FileService.class);

                            filesystemStoreImpl = spy(new DefaultFileSystemStoreImpl<>(
                                loader, null, placer, fileService));

                            writeableResource = mock(WritableResource.class);
                            entity = new SingleContentFields();
                            content = new ByteArrayInputStream(
                                "Hello content world!".getBytes());

                            when(placer.convert(eq(entity), eq(String.class))).thenReturn(null);

                            when(placer.convert(eq(SAMPLE_CONTENT_ID),
                                eq(String.class)))
                                .thenReturn(SAMPLE_CONTENT_ID);

                            when(placer.convert(matches(
                                    "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"),
                                eq(String.class)))
                                .thenReturn(SAMPLE_CONTENT_ID);

                            when(loader.getResource(eq(SAMPLE_CONTENT_ID)))
                                .thenReturn(writeableResource);
                            output = mock(OutputStream.class);
                            when(writeableResource.getOutputStream()).thenReturn(output);

                            when(writeableResource.contentLength()).thenReturn(20L);

                            entity.setContentId(SAMPLE_CONTENT_ID);
                            when(writeableResource.exists()).thenReturn(true);

                            try {
                                filesystemStoreImpl.setContent(entity, content);
                            } catch (Exception e) {
                                DefaultFileSystemStoresImplTest.this.e = e;
                            }
                        }

                        @Test
                        void shouldUseThePlacerServiceToGetAResourcePath() {
                            verify(placer, atLeastOnce()).convert(any(), eq(String.class));
                            verify(loader).getResource(eq(SAMPLE_CONTENT_ID));
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

                    @DisplayName("When the content does not already exist")

                    class ContentDoesNotAlreadyExist {
                        @BeforeEach
                        void setUp() throws IOException {
                            loader = mock(FileSystemResourceLoader.class);
                            placer = mock(PlacementService.class);
                            fileService = mock(FileService.class);

                            filesystemStoreImpl = spy(new DefaultFileSystemStoreImpl<>(
                                loader, null, placer, fileService));

                            writeableResource = mock(WritableResource.class);
                            entity = new SingleContentFields();
                            content = new ByteArrayInputStream(
                                "Hello content world!".getBytes());

                            when(placer.convert(eq(entity), eq(String.class))).thenReturn(null);

                            when(placer.convert(eq(SAMPLE_CONTENT_ID),
                                eq(String.class)))
                                .thenReturn(SAMPLE_CONTENT_ID);

                            when(placer.convert(matches(
                                    "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"),
                                eq(String.class)))
                                .thenReturn(SAMPLE_CONTENT_ID);

                            when(loader.getResource(eq(SAMPLE_CONTENT_ID)))
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
                                filesystemStoreImpl.setContent(entity, content);
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
                            verify(loader).getResource(eq(SAMPLE_CONTENT_ID));
                        }

                        @Test
                        void shouldWriteToTheResourceSOutputStream() throws IOException {
                            verify(writeableResource).getOutputStream();
                            verify(output, times(1)).write(ArgumentMatchers.any(), eq(0),
                                eq(20));
                        }
                    }

                    @Nested

                    @DisplayName("When getting the resource output stream throws an io exception")

                    class GettingResourceOutputStreamThrowsIOException {
                        @BeforeEach
                        void setUp() throws IOException {
                            loader = mock(FileSystemResourceLoader.class);
                            placer = mock(PlacementService.class);
                            fileService = mock(FileService.class);

                            filesystemStoreImpl = spy(new DefaultFileSystemStoreImpl<>(
                                loader, null, placer, fileService));

                            writeableResource = mock(WritableResource.class);
                            entity = new SingleContentFields();
                            content = new ByteArrayInputStream(
                                "Hello content world!".getBytes());

                            when(placer.convert(eq(entity), eq(String.class))).thenReturn(null);

                            when(placer.convert(eq(SAMPLE_CONTENT_ID),
                                eq(String.class)))
                                .thenReturn(SAMPLE_CONTENT_ID);

                            when(placer.convert(matches(
                                    "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"),
                                eq(String.class)))
                                .thenReturn(SAMPLE_CONTENT_ID);

                            when(loader.getResource(eq(SAMPLE_CONTENT_ID)))
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
                                filesystemStoreImpl.setContent(entity, content);
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

                    filesystemStoreImpl = spy(new DefaultFileSystemStoreImpl<>(
                        loader, null, placer, fileService));

                    entity = new SingleContentFields();
                    content = new ByteArrayInputStream("Hello content world!".getBytes());
                    inputResource = new InputStreamResource(content);
                }

                @Test
                void shouldDelegateToSetContentFromInputStream() {
                    try {
                        filesystemStoreImpl.setContent(entity, inputResource);
                    } catch (Throwable ignored) {
                        // delegated setContent(InputStream) runs before getResource assert/placement fails
                    }
                    verify(filesystemStoreImpl).setContent(eq(entity), eq(content));
                }

                @Nested

                @DisplayName("When the resource throws an io exception")

                class ResourceThrowsIOException {
                    @BeforeEach
                    void setUp() throws IOException {
                        loader = mock(FileSystemResourceLoader.class);
                        placer = mock(PlacementService.class);
                        fileService = mock(FileService.class);

                        filesystemStoreImpl = spy(new DefaultFileSystemStoreImpl<>(
                            loader, null, placer, fileService));

                        writeableResource = mock(WritableResource.class);
                        entity = new SingleContentFields();
                        content = new ByteArrayInputStream("Hello content world!".getBytes());
                        inputResource = new InputStreamResource(content);

                        inputResource = mock(Resource.class);
                        when(inputResource.getInputStream()).thenThrow(new IOException("setContent badness"));

                        try {
                            filesystemStoreImpl.setContent(entity, inputResource);
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
                @DisplayName("Given an entity converter")
                class EntityConverter {
                    @Nested
                    @DisplayName("When the resource does not exists")
                    class ResourceDoesNotExists {
                        @BeforeEach
                        void setUp() throws IOException {
                            loader = mock(FileSystemResourceLoader.class);
                            placer = mock(PlacementService.class);
                            fileService = mock(FileService.class);

                            filesystemStoreImpl = spy(new DefaultFileSystemStoreImpl<>(
                                loader, null, placer, fileService));

                            writeableResource = mock(WritableResource.class);
                            entity = new SingleContentFields();
                            content = mock(InputStream.class);
                            entity.setContentId(SAMPLE_CONTENT_ID);

                            when(placer.convert(eq(entity), eq(String.class)))
                                .thenReturn(null);

                            when(placer.convert(eq(SAMPLE_CONTENT_ID), eq(String.class)))
                                .thenReturn(SAMPLE_CONTENT_ID);

                            when(loader.getResource(eq(SAMPLE_CONTENT_ID)))
                                .thenReturn(writeableResource);
                            when(writeableResource.getInputStream()).thenReturn(content);

                            when(placer.canConvert(eq(entity.getClass()), eq(String.class))).thenReturn(true);
                            when(placer.convert(eq(entity), eq(String.class)))
                                .thenReturn(SAMPLE_FS_RESOURCE_PATH);

                            nonExistentResource = mock(DeletableResource.class);

                            when(loader.getResource(eq(SAMPLE_FS_RESOURCE_PATH)))
                                .thenReturn(nonExistentResource);

                            when(writeableResource.exists()).thenReturn(false);

                            try {
                                result = filesystemStoreImpl.getContent(entity);
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

                    @DisplayName("When the resource exists")

                    class ResourceExists {
                        @BeforeEach
                        void setUp() throws IOException {
                            loader = mock(FileSystemResourceLoader.class);
                            placer = mock(PlacementService.class);
                            fileService = mock(FileService.class);

                            filesystemStoreImpl = spy(new DefaultFileSystemStoreImpl<>(
                                loader, null, placer, fileService));

                            writeableResource = mock(WritableResource.class);
                            entity = new SingleContentFields();
                            content = mock(InputStream.class);
                            entity.setContentId(SAMPLE_CONTENT_ID);

                            when(placer.convert(eq(entity), eq(String.class)))
                                .thenReturn(null);

                            when(placer.convert(eq(SAMPLE_CONTENT_ID), eq(String.class)))
                                .thenReturn(SAMPLE_CONTENT_ID);

                            when(loader.getResource(eq(SAMPLE_CONTENT_ID)))
                                .thenReturn(writeableResource);
                            when(writeableResource.getInputStream()).thenReturn(content);

                            when(placer.canConvert(eq(entity.getClass()), eq(String.class))).thenReturn(true);
                            when(placer.convert(eq(entity), eq(String.class)))
                                .thenReturn(SAMPLE_FS_RESOURCE_PATH);

                            when(loader.getResource(eq(SAMPLE_FS_RESOURCE_PATH)))
                                .thenReturn(writeableResource);

                            when(writeableResource.exists()).thenReturn(true);

                            when(writeableResource.getInputStream()).thenReturn(content);

                            try {
                                result = filesystemStoreImpl.getContent(entity);
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

                @DisplayName("Given just the default id converter")

                class JustDefaultIDConverter {
                    @Nested
                    @DisplayName("When the resource does not exists")
                    class ResourceDoesNotExists {
                        @BeforeEach
                        void setUp() throws IOException {
                            loader = mock(FileSystemResourceLoader.class);
                            placer = mock(PlacementService.class);
                            fileService = mock(FileService.class);

                            filesystemStoreImpl = spy(new DefaultFileSystemStoreImpl<>(
                                loader, null, placer, fileService));

                            writeableResource = mock(WritableResource.class);
                            entity = new SingleContentFields();
                            content = mock(InputStream.class);
                            entity.setContentId(SAMPLE_CONTENT_ID);

                            when(placer.convert(eq(entity), eq(String.class))).thenReturn(null);

                            when(placer.convert(eq(SAMPLE_CONTENT_ID), eq(String.class)))
                                .thenReturn(SAMPLE_CONTENT_ID);

                            when(loader.getResource(eq(SAMPLE_CONTENT_ID))).thenReturn(writeableResource);
                            when(writeableResource.getInputStream()).thenReturn(content);

                            when(placer.convert(eq(entity), eq(String.class))).thenReturn(null);
                            nonExistentResource = mock(DeletableResource.class);
                            when(writeableResource.exists()).thenReturn(true);

                            when(loader.getResource(eq(SAMPLE_FS_RESOURCE_PATH))).thenReturn(nonExistentResource);
                            when(loader.getResource(eq(SAMPLE_CONTENT_ID))).thenReturn(nonExistentResource);

                            try {
                                result = filesystemStoreImpl.getContent(entity);
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

                    @DisplayName("When the resource exists")

                    class ResourceExists {
                        @BeforeEach
                        void setUp() throws IOException {
                            loader = mock(FileSystemResourceLoader.class);
                            placer = mock(PlacementService.class);
                            fileService = mock(FileService.class);

                            filesystemStoreImpl = spy(new DefaultFileSystemStoreImpl<>(
                                loader, null, placer, fileService));

                            writeableResource = mock(WritableResource.class);
                            entity = new SingleContentFields();
                            content = mock(InputStream.class);
                            entity.setContentId(SAMPLE_CONTENT_ID);

                            when(placer.convert(eq(entity), eq(String.class)))
                                .thenReturn(null);

                            when(placer.convert(eq(SAMPLE_CONTENT_ID), eq(String.class)))
                                .thenReturn(SAMPLE_CONTENT_ID);

                            when(loader.getResource(eq(SAMPLE_CONTENT_ID)))
                                .thenReturn(writeableResource);
                            when(writeableResource.getInputStream()).thenReturn(content);

                            when(placer.convert(eq(entity), eq(String.class))).thenReturn(null);
                            when(writeableResource.exists()).thenReturn(true);
                            try {
                                result = filesystemStoreImpl.getContent(entity);
                            } catch (Exception e) {
                                DefaultFileSystemStoresImplTest.this.e = e;
                            }
                        }

                        @Test
                        void shouldGetContent() {
                            assertThat(result).isEqualTo(content);
                        }

                        @Nested

                        @DisplayName("When getting the resource input stream throws an io exception")

                        class GettingResourceInputStreamThrowsIOException {
                            @BeforeEach
                            void setUp() throws IOException {
                                loader = mock(FileSystemResourceLoader.class);
                                placer = mock(PlacementService.class);
                                fileService = mock(FileService.class);

                                filesystemStoreImpl = spy(new DefaultFileSystemStoreImpl<>(
                                    loader, null, placer, fileService));

                                writeableResource = mock(WritableResource.class);
                                entity = new SingleContentFields();
                                content = mock(InputStream.class);
                                entity.setContentId(SAMPLE_CONTENT_ID);

                                when(placer.convert(eq(entity), eq(String.class)))
                                    .thenReturn(null);

                                when(placer.convert(eq(SAMPLE_CONTENT_ID), eq(String.class)))
                                    .thenReturn(SAMPLE_CONTENT_ID);

                                when(loader.getResource(eq(SAMPLE_CONTENT_ID)))
                                    .thenReturn(writeableResource);
                                when(writeableResource.getInputStream()).thenReturn(content);

                                when(placer.convert(eq(entity), eq(String.class))).thenReturn(null);
                                when(writeableResource.exists()).thenReturn(true);
                                when(writeableResource.getInputStream()).thenThrow(new IOException("test-ioexception"));
                                try {
                                    result = filesystemStoreImpl.getContent(entity);
                                } catch (Exception e) {
                                    DefaultFileSystemStoresImplTest.this.e = e;
                                }
                            }

                            @Test
                            void shouldReturnAStoreAccessExceptionWrappingTheIOException() {
                                assertThat(e).isInstanceOf(StoreAccessException.class);
                                assertThat(e.getCause().getMessage()).isEqualTo("test-ioexception");
                            }
                        }
                    }

                    @Nested

                    @DisplayName("When the resource exists but in the old location")

                    class ResourceExistsButInOldLocation {
                        @BeforeEach
                        void setUp() throws IOException {
                            loader = mock(FileSystemResourceLoader.class);
                            placer = mock(PlacementService.class);
                            fileService = mock(FileService.class);

                            filesystemStoreImpl = spy(new DefaultFileSystemStoreImpl<>(
                                loader, null, placer, fileService));

                            writeableResource = mock(WritableResource.class);
                            entity = new SingleContentFields();
                            content = mock(InputStream.class);
                            entity.setContentId(SAMPLE_CONTENT_ID);

                            when(placer.convert(eq(entity), eq(String.class)))
                                .thenReturn(null);

                            when(placer.convert(eq(SAMPLE_CONTENT_ID), eq(String.class)))
                                .thenReturn(SAMPLE_CONTENT_ID);

                            when(loader.getResource(eq(SAMPLE_CONTENT_ID)))
                                .thenReturn(writeableResource);
                            when(writeableResource.getInputStream()).thenReturn(content);

                            when(placer.convert(eq(entity), eq(String.class))).thenReturn(null);
                            nonExistentResource = mock(DeletableResource.class);
                            when(loader.getResource(eq(SAMPLE_FS_RESOURCE_PATH)))
                                .thenReturn(nonExistentResource);
                            when(nonExistentResource.exists()).thenReturn(false);

                            when(loader.getResource(eq(SAMPLE_CONTENT_ID)))
                                .thenReturn(writeableResource);
                            when(writeableResource.exists()).thenReturn(true);

                            try {
                                result = filesystemStoreImpl.getContent(entity);
                            } catch (Exception e) {
                                DefaultFileSystemStoresImplTest.this.e = e;
                            }
                        }

                        @Test
                        void shouldCheckTheNewLocationAndThenTheOld() {
                            InOrder inOrder = Mockito.inOrder(loader);

                            inOrder.verify(loader).getResource(eq(SAMPLE_CONTENT_ID));
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
                @DisplayName("Given an entity converter")
                class EntityConverter {
                    @Nested
                    @DisplayName("Given the resource does not exist")
                    class ResourceDoesNotExist {
                        @BeforeEach
                        void setUp() {
                            loader = mock(FileSystemResourceLoader.class);
                            placer = mock(PlacementService.class);
                            fileService = mock(FileService.class);

                            filesystemStoreImpl = spy(new DefaultFileSystemStoreImpl<>(
                                loader, null, placer, fileService));

                            writeableResource = mock(WritableResource.class);
                            entity = new SingleContentFields();
                            entity.setContentId(SAMPLE_CONTENT_ID);
                            entity.setContentLen(100L);
                            deletableResource = mock(DeletableResource.class);

                            when(placer.canConvert(eq(entity.getClass()), eq(String.class))).thenReturn(true);
                            when(placer.convert(eq(entity), eq(String.class)))
                                .thenReturn(SAMPLE_FS_RESOURCE_PATH);

                            nonExistentResource = mock(DeletableResource.class);

                            when(loader.getResource(eq(SAMPLE_FS_RESOURCE_PATH)))
                                .thenReturn(nonExistentResource);

                            when(nonExistentResource.exists()).thenReturn(false);

                            filesystemStoreImpl.unsetContent(entity);
                        }

                        @Test
                        void shouldNotDeleteTheResource() throws IOException {
                            verify(nonExistentResource, never()).delete();
                        }
                    }

                    @Nested

                    @DisplayName("Given the resource exists")

                    class ResourceExists {
                        @BeforeEach
                        void setUp() throws IOException {
                            loader = mock(FileSystemResourceLoader.class);
                            placer = mock(PlacementService.class);
                            fileService = mock(FileService.class);

                            filesystemStoreImpl = spy(new DefaultFileSystemStoreImpl<>(
                                loader, null, placer, fileService));

                            writeableResource = mock(WritableResource.class);
                            entity = new SingleContentFields();
                            entity.setContentId(SAMPLE_CONTENT_ID);
                            entity.setContentLen(100L);
                            deletableResource = mock(DeletableResource.class);

                            when(placer.canConvert(eq(entity.getClass()), eq(String.class))).thenReturn(true);
                            when(placer.convert(eq(entity), eq(String.class)))
                                .thenReturn(SAMPLE_FS_RESOURCE_PATH);

                            deletableResource = mock(DeletableResource.class);

                            when(loader.getResource(eq(SAMPLE_FS_RESOURCE_PATH)))
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

                            filesystemStoreImpl.unsetContent(entity);
                        }

                        @Test
                        void shouldDeleteTheResource() throws IOException {
                            verify(deletableResource, times(1)).delete();
                        }
                    }
                }

                @Nested

                @DisplayName("Given just the default id converter")

                class JustDefaultIDConverter {
                    @Nested
                    @DisplayName("When the content exists in the new location")
                    class ContentExistsInNewLocation {
                        @BeforeEach
                        void setUp() throws IOException {
                            loader = mock(FileSystemResourceLoader.class);
                            placer = mock(PlacementService.class);
                            fileService = mock(FileService.class);

                            filesystemStoreImpl = spy(new DefaultFileSystemStoreImpl<>(
                                loader, null, placer, fileService));

                            writeableResource = mock(WritableResource.class);
                            entity = new SingleContentFields();
                            entity.setContentId(SAMPLE_CONTENT_ID);
                            entity.setContentLen(100L);
                            deletableResource = mock(DeletableResource.class);

                            when(placer.convert(eq(entity), eq(String.class))).thenReturn(null);
                            when(placer.convert(eq(SAMPLE_CONTENT_ID), eq(String.class)))
                                .thenReturn(SAMPLE_CONTENT_ID);

                            when(loader.getResource(eq(SAMPLE_CONTENT_ID)))
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

                            filesystemStoreImpl.unsetContent(entity);
                        }

                        @Test
                        void shouldDeleteTheResource() throws IOException {
                            verify(deletableResource, times(1)).delete();
                        }

                        @Nested

                        @DisplayName("When the property has a dedicated content ID field")

                        class PropertyHasDedicatedContentIdField {
                            @BeforeEach
                            void setUp() throws IOException {
                                loader = mock(FileSystemResourceLoader.class);
                                placer = mock(PlacementService.class);
                                fileService = mock(FileService.class);

                                filesystemStoreImpl = spy(new DefaultFileSystemStoreImpl<>(
                                    loader, null, placer, fileService));

                                writeableResource = mock(WritableResource.class);
                                entity = new SingleContentFields();
                                entity.setContentId(SAMPLE_CONTENT_ID);
                                entity.setContentLen(100L);
                                deletableResource = mock(DeletableResource.class);

                                when(placer.convert(eq(entity), eq(String.class))).thenReturn(null);
                                when(placer.convert(eq(SAMPLE_CONTENT_ID), eq(String.class)))
                                    .thenReturn(SAMPLE_CONTENT_ID);

                                when(loader.getResource(eq(SAMPLE_CONTENT_ID)))
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

                                filesystemStoreImpl.unsetContent(entity);
                            }

                            @Test
                            void shouldResetTheMetadata() {
                                assertThat(entity.getContentId()).isNull();
                                assertThat(entity.getContentLen()).isNull();
                            }
                        }

                        @Nested

                        @DisplayName("When the property s content ID field also is the javax persistence id field")

                        class PropertySContentIdFieldAlsoIsJavaxPersistenceIdField {
                            @BeforeEach
                            void setUp() throws IOException {
                                loader = mock(FileSystemResourceLoader.class);
                                placer = mock(PlacementService.class);
                                fileService = mock(FileService.class);

                                filesystemStoreImpl = spy(new DefaultFileSystemStoreImpl<>(
                                    loader, null, placer, fileService));

                                writeableResource = mock(WritableResource.class);
                                entity = new SingleContentFields();
                                entity.setContentId(SAMPLE_CONTENT_ID);
                                entity.setContentLen(100L);
                                deletableResource = mock(DeletableResource.class);

                                when(placer.convert(eq(entity), eq(String.class))).thenReturn(null);
                                when(placer.convert(eq(SAMPLE_CONTENT_ID), eq(String.class)))
                                    .thenReturn(SAMPLE_CONTENT_ID);

                                when(loader.getResource(eq(SAMPLE_CONTENT_ID)))
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

                                entity = new SharedIdJakartaJpaEntity();
                                entity.setContentId(SAMPLE_CONTENT_ID);

                                filesystemStoreImpl.unsetContent(entity);
                            }

                            @Test
                            void shouldNotResetTheContentIdMetadata() {
                                assertThat(entity.getContentId()).isEqualTo(SAMPLE_CONTENT_ID);
                                assertThat(entity.getContentLen()).isNull();
                            }
                        }

                        @Nested

                        @DisplayName("When the property s content ID field also is the spring id field")

                        class PropertySContentIdFieldAlsoIsSpringIdField {
                            @BeforeEach
                            void setUp() throws IOException {
                                loader = mock(FileSystemResourceLoader.class);
                                placer = mock(PlacementService.class);
                                fileService = mock(FileService.class);

                                filesystemStoreImpl = spy(new DefaultFileSystemStoreImpl<>(
                                    loader, null, placer, fileService));

                                writeableResource = mock(WritableResource.class);
                                entity = new SingleContentFields();
                                entity.setContentId(SAMPLE_CONTENT_ID);
                                entity.setContentLen(100L);
                                deletableResource = mock(DeletableResource.class);

                                when(placer.convert(eq(entity), eq(String.class))).thenReturn(null);
                                when(placer.convert(eq(SAMPLE_CONTENT_ID), eq(String.class)))
                                    .thenReturn(SAMPLE_CONTENT_ID);

                                when(loader.getResource(eq(SAMPLE_CONTENT_ID)))
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

                                entity = new SharedIdSpringJpaEntity();
                                entity.setContentId(SAMPLE_CONTENT_ID);

                                filesystemStoreImpl.unsetContent(entity);
                            }

                            @Test
                            void shouldNotResetTheContentIdMetadata() {
                                assertThat(entity.getContentId()).isEqualTo(SAMPLE_CONTENT_ID);
                                assertThat(entity.getContentLen()).isNull();
                            }
                        }
                    }

                    @Nested

                    @DisplayName("When the content doesnt exist")

                    class ContentDoesntExist {
                        @BeforeEach
                        void setUp() {
                            loader = mock(FileSystemResourceLoader.class);
                            placer = mock(PlacementService.class);
                            fileService = mock(FileService.class);

                            filesystemStoreImpl = spy(new DefaultFileSystemStoreImpl<>(
                                loader, null, placer, fileService));

                            writeableResource = mock(WritableResource.class);
                            entity = new SingleContentFields();
                            entity.setContentId(SAMPLE_CONTENT_ID);
                            entity.setContentLen(100L);
                            deletableResource = mock(DeletableResource.class);

                            when(placer.convert(eq(entity), eq(String.class))).thenReturn(null);
                            when(placer.convert(eq(SAMPLE_CONTENT_ID), eq(String.class)))
                                .thenReturn(SAMPLE_CONTENT_ID);

                            nonExistentResource = mock(DeletableResource.class);
                            when(loader.getResource(eq(SAMPLE_CONTENT_ID)))
                                .thenReturn(nonExistentResource);
                            when(nonExistentResource.exists()).thenReturn(false);

                            filesystemStoreImpl.unsetContent(entity);
                        }

                        @Test
                        void shouldUnsetTheContent() throws IOException {
                            verify(nonExistentResource, never()).delete();
                            assertThat(entity.getContentId()).isNull();
                            assertThat(entity.getContentLen()).isNull();
                        }
                    }
                }
            }
        }
    }
}
