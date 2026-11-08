package internal.org.springframework.content.mongo.io;

import com.mongodb.client.gridfs.model.GridFSFile;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GridFSResourceTest {

    private GridFsStoreResource r;

    private String location;
    private GridFsTemplate gridfs;

    private GridFSFile file;

    private Object rc;

    @BeforeEach
    void createTemplate() {
        location = "some-location";
        gridfs = mock(GridFsTemplate.class);
        r = new GridFsStoreResource(location, gridfs);
    }

    @Nested
    class Resource {

        @Nested
        class ContentLength {
            @BeforeEach
            void mockFile() {
                file = mock(GridFSFile.class);
            }

            @Nested
            class GivenTheFileExists {
                @BeforeEach
                void fileExists() {
                    when(gridfs.findOne(any())).thenReturn(file);
                }

                @Test
                void shouldReturnTheFilesLength() throws Exception {
                    r.contentLength();
                    verify(file).getLength();
                }
            }

            @Nested
            class GivenTheFileDoesntExist {
                @Test
                void shouldReturnZero() throws Exception {
                    rc = r.contentLength();
                    verify(file, never()).getLength();
                    assertThat(rc).isEqualTo(0L);
                }
            }
        }

        @Nested
        class GetFilename {
            @Test
            void shouldReturnTheLocation() {
                assertThat(r.getFilename()).isEqualTo("some-location");
            }
        }

        @Nested
        class GetId {
            @BeforeEach
            void mockFile() {
                file = mock(GridFSFile.class);
            }

            @Nested
            class GivenTheFileExists {
                @BeforeEach
                void fileExists() {
                    when(gridfs.findOne(any())).thenReturn(file);
                }

                @Test
                void shouldReturnTheFilesId() {
                    r.getId();
                    verify(file).getId();
                }
            }

            @Nested
            class GivenTheFileDoesntExist {
                @Test
                void shouldReturnNull() {
                    rc = r.getId();
                    verify(file, never()).getId();
                    assertThat(rc).isNull();
                }
            }
        }

        @Nested
        class Exists {
            @Nested
            class GivenTheFileExists {
                @BeforeEach
                void fileExists() {
                    file = mock(GridFSFile.class);
                    when(gridfs.findOne(any())).thenReturn(file);
                }

                @Test
                void shouldReturnTrue() {
                    assertThat(r.exists()).isTrue();
                }
            }

            @Nested
            class GivenTheFileDoesntExist {
                @Test
                void shouldReturnFalse() {
                    assertThat(r.exists()).isFalse();
                }
            }
        }

        @Test
        void isOpenShouldReturnTrue() {
            assertThat(r.isOpen()).isTrue();
        }

        @Nested
        class GetInputStream {
            @Nested
            class GivenTheFileExists {
                @BeforeEach
                void fileExists() {
                    file = mock(GridFSFile.class);
                    when(gridfs.findOne(any())).thenReturn(file);
                    when(gridfs.getResource(location)).thenReturn(mock(GridFsResource.class));
                }

                @Test
                void shouldReturnTheFilesInputStream() throws Exception {
                    r.getInputStream();
                    verify(gridfs).getResource(location);
                }
            }

            @Nested
            class GivenTheFileDoesntExist {
                @Test
                void shouldReturnNull() throws Exception {
                    assertThat(r.getInputStream()).isNull();
                    verify(gridfs, never()).getResource(location);
                }
            }
        }

        @Test
        void getDescriptionShouldReturnSomething() {
            assertThat(r.getDescription()).isNotNull();
        }

        @Test
        void isReadableShouldReturnTrue() {
            assertThat(r.isReadable()).isTrue();
        }

        @Nested
        class LastModified {
            @BeforeEach
            void mockFile() {
                file = mock(GridFSFile.class);
            }

            @Nested
            class GivenTheFileExists {
                @BeforeEach
                void fileExists() {
                    when(gridfs.findOne(any())).thenReturn(file);
                    when(file.getUploadDate()).thenReturn(new Date());
                }

                @Test
                void shouldReturnTheFilesUploadDate() throws Exception {
                    r.lastModified();
                    verify(file).getUploadDate();
                }
            }

            @Nested
            class GivenTheFileDoesntExist {
                @Test
                void shouldReturnMinusOne() throws Exception {
                    rc = r.lastModified();
                    verify(file, never()).getUploadDate();
                    assertThat(rc).isEqualTo(-1L);
                }
            }
        }
    }

    @Nested
    class WritableResource {
        @Test
        void isWritableShouldReturnTrue() {
            assertThat(r.isWritable()).isTrue();
        }

        @Nested
        class GetOutputStream {
            @Test
            void shouldStoreTheContentAndDeleteExistingContent() throws Exception {
                OutputStream out = r.getOutputStream();
                out.write(new byte[] { 32 }, 0, 1);
                IOUtils.closeQuietly(out);

                verify(gridfs).store(any(InputStream.class), eq(location));
                verify(gridfs).delete(any());
            }
        }
    }

    @Nested
    class DeletableResource {
        @Nested
        class Delete {
            @Nested
            class GivenTheFileExists {
                @BeforeEach
                void fileExists() {
                    file = mock(GridFSFile.class);
                    when(gridfs.findOne(any())).thenReturn(file);
                }

                @Test
                void shouldDeleteTheFile() {
                    r.delete();
                    verify(gridfs).delete(any());
                }
            }

            @Nested
            class GivenTheFileDoesntExist {
                @Test
                void shouldNotDelete() {
                    r.delete();
                    verify(gridfs, never()).delete(any());
                }
            }
        }
    }
}
