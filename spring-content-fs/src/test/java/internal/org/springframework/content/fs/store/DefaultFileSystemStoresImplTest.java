package internal.org.springframework.content.fs.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.commons.store.StoreEntityValidation;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
                void shouldUsePlacerServiceToGetResourcePath() {
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
                    void shouldNotReturnResource() {
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
                    void shouldUsePlacerServiceToGetResourcePath() {
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
                    void shouldNotNeedToConvertId() {
                        verify(placer, never()).convert(not(eq(entity)), eq(String.class));
                    }

                    @Test
                    void shouldReturnResource() {
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
                    void shouldNotCallPlacementServiceTryingToConvertEntityToString() {
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
                void shouldSetEntityContentIdAttribute() {
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
                void shouldResetEntityContentIdAttribute() {
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
                    @DisplayName("When the content doesn't yet exist")
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
                        void shouldMakeNewUUID() {
                            assertThat(entity.getContentId()).isNotNull();
                        }

                        @Test
                        void shouldCreateNewResource() {
                            verify(loader).getResource(eq(SAMPLE_CONTENT_ID));
                        }

                        @Test
                        void shouldWriteToResourceOutputStream() throws IOException {
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
                        void shouldWriteToResourceOutputStream() throws IOException {
                            verify(output, times(1)).write(ArgumentMatchers.any(), eq(0),
                                eq(20));
                            verify(output).close();
                        }

                        @Test
                        void shouldChangeContentLength() {
                            assertThat(entity.getContentLen()).isEqualTo(20L);
                        }
                    }
                }

                @Nested
                @DisplayName("Given just the default ID converters")
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
                        void shouldUsePlacerServiceToGetResourcePath() {
                            verify(placer, atLeastOnce()).convert(any(), eq(String.class));
                            verify(loader).getResource(eq(SAMPLE_CONTENT_ID));
                        }

                        @Test
                        void shouldChangeContentLength() {
                            assertThat(entity.getContentLen()).isEqualTo(20L);
                        }

                        @Test
                        void shouldWriteToResourceOutputStream() throws IOException {
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
                        void shouldMakeNewUUID() {
                            assertThat(entity.getContentId()).isNotNull();
                        }

                        @Test
                        void shouldCreateNewResource() {
                            verify(loader).getResource(eq(SAMPLE_CONTENT_ID));
                        }

                        @Test
                        void shouldWriteToResourceOutputStream() throws IOException {
                            verify(writeableResource).getOutputStream();
                            verify(output, times(1)).write(ArgumentMatchers.any(), eq(0),
                                eq(20));
                        }
                    }

                    @Nested
                    @DisplayName("When getting the resource output stream throws an I/O exception")
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
                        void shouldReturnStoreAccessExceptionWrappingIOException() {
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
                @DisplayName("When the resource throws an I/O exception")
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
                    void shouldThrowStoreAccessException() {
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
                        void shouldNotReturnContent() {
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
                @DisplayName("Given just the default ID converter")
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
                        void shouldNotFindContent() {
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
                        @DisplayName("When getting the resource input stream throws an I/O exception")
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
                            void shouldReturnStoreAccessExceptionWrappingIOException() {
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
                        void shouldCheckNewLocationAndThenOld() {
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
                        void shouldNotDeleteResource() throws IOException {
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
                        void shouldDeleteResource() throws IOException {
                            verify(deletableResource, times(1)).delete();
                        }
                    }
                }

                @Nested
                @DisplayName("Given just the default ID converter")
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
                        void shouldDeleteResource() throws IOException {
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
                            void shouldResetMetadata() {
                                assertThat(entity.getContentId()).isNull();
                                assertThat(entity.getContentLen()).isNull();
                            }
                        }

                        @Nested
                        @DisplayName("When the property's content ID field also is the javax.persistence ID field")
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
                            void shouldNotResetContentIdMetadata() {
                                assertThat(entity.getContentId()).isEqualTo(SAMPLE_CONTENT_ID);
                                assertThat(entity.getContentLen()).isNull();
                            }
                        }

                        @Nested
                        @DisplayName("When the property's content ID field also is the Spring ID field")
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
                            void shouldNotResetContentIdMetadata() {
                                assertThat(entity.getContentId()).isEqualTo(SAMPLE_CONTENT_ID);
                                assertThat(entity.getContentLen()).isNull();
                            }
                        }
                    }

                    @Nested
                    @DisplayName("When the content doesn't exist")
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
                        void shouldUnsetContent() throws IOException {
                            verify(nonExistentResource, never()).delete();
                            assertThat(entity.getContentId()).isNull();
                            assertThat(entity.getContentLen()).isNull();
                        }
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("Given a null entity on the store implementation")
    @SuppressWarnings("DataFlowIssue")
    class NullEntityOnDirectImpl {

        private DefaultFileSystemStoreImpl<SingleContentFields, String> store;

        @BeforeEach
        void setUp() {
            store = new DefaultFileSystemStoreImpl<>(
                mock(FileSystemResourceLoader.class),
                null,
                new PlacementServiceImpl(),
                mock(FileService.class));
        }

        @Test
        void getContentRejectsNullEntity() {
            assertNullEntityRejected(() -> store.getContent(null));
        }

        @Test
        void setContentRejectsNullEntity() {
            assertNullEntityRejected(() -> store.setContent(null, InputStream.nullInputStream()));
        }

        @Test
        void getResourceRejectsNullEntity() {
            assertNullEntityRejected(() -> store.getResource((SingleContentFields) null));
        }

        @Test
        void getResourceWithPropertyPathRejectsNullEntity() {
            assertNullEntityRejected(() -> store.getResource(null, PropertyPath.from("content")));
        }

        @Test
        void getResourceWithPropertyPathRejectsNullPropertyPath() {
            assertThatThrownBy(() -> store.getResource(new SingleContentFields(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(StoreEntityValidation.PROPERTY_PATH_MUST_NOT_BE_NULL);
        }

        @Test
        void getResourceWithParamsRejectsNullGetResourceParams() {
            assertThatThrownBy(() -> store.getResource(
                new SingleContentFields(), PropertyPath.from("content"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(StoreEntityValidation.GET_RESOURCE_PARAMS_MUST_NOT_BE_NULL);
        }

        @Test
        void associateWithPropertyPathRejectsNullPropertyPath() {
            assertThatThrownBy(() -> store.associate(new SingleContentFields(), null, SAMPLE_CONTENT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(StoreEntityValidation.PROPERTY_PATH_MUST_NOT_BE_NULL);
        }

        @Test
        void setContentRejectsNullContent() {
            assertThatThrownBy(() -> store.setContent(new SingleContentFields(), (InputStream) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(StoreEntityValidation.CONTENT_MUST_NOT_BE_NULL);
        }

        @Test
        void associateRejectsNullEntity() {
            assertNullEntityRejected(() -> store.associate(null, SAMPLE_CONTENT_ID));
        }

        @Test
        void associateRejectsNullContentId() {
            assertThatThrownBy(() -> store.associate(new SingleContentFields(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(StoreEntityValidation.CONTENT_ID_MUST_NOT_BE_NULL);
        }

        @Test
        void associateWithPropertyPathRejectsNullContentId() {
            assertThatThrownBy(() -> store.associate(new SingleContentFields(), PropertyPath.from("content"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(StoreEntityValidation.CONTENT_ID_MUST_NOT_BE_NULL);
        }

        @Test
        void unassociateRejectsNullEntity() {
            assertNullEntityRejected(() -> store.unassociate(null));
        }

        @Test
        void unassociateWithPropertyPathRejectsNullEntity() {
            assertNullEntityRejected(() -> store.unassociate(null, PropertyPath.from("content")));
        }

        @Test
        void unsetContentRejectsNullEntity() {
            assertNullEntityRejected(() -> store.unsetContent(null));
        }

        private static void assertNullEntityRejected(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
            assertThatThrownBy(call)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(StoreEntityValidation.ENTITY_MUST_NOT_BE_NULL);
        }
    }
}
