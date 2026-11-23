package internal.org.springframework.content.commons.store.factory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.context.ApplicationEventPublisher;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StoreImplTest {

    private StoreImpl stores;

    private ContentStore<Object, Serializable> store;
    private ApplicationEventPublisher publisher;
    private Path contentCopyPathRoot;

    @Nested
    class StoreImplCases {
        @Nested
        class SetContentInputStream {
            @BeforeEach
            void setUp() throws IOException {
                store = mock();
                publisher = mock(ApplicationEventPublisher.class);

                when(store.setContent(any(), any(InputStream.class))).thenReturn(new Object());

                contentCopyPathRoot = Files.createTempDirectory("storeImplTest");

                for (File f : Objects.requireNonNull(contentCopyPathRoot.toFile().listFiles())) {
                    if (f.getName().endsWith(".tmp")) {
                        Files.deleteIfExists(f.toPath());
                    }
                }

                stores = new StoreImpl(store, publisher, contentCopyPathRoot);

                stores.setContent(new Object(), new ByteArrayInputStream("foo".getBytes()));
            }

            @Test
            void shouldDeleteTheContentCopyFile() {
                assertThat(Arrays.asList(Objects.requireNonNull(contentCopyPathRoot.toFile().listFiles())))
                    .filteredOn(f -> f.getName().endsWith(".tmp"))
                    .isEmpty();
            }
        }

        @Nested
        class GetContent {
            @BeforeEach
            void setUp() throws IOException {
                store = mock();
                publisher = mock(ApplicationEventPublisher.class);

                contentCopyPathRoot = Files.createTempDirectory("storeImplTest");

                for (File f : Objects.requireNonNull(contentCopyPathRoot.toFile().listFiles())) {
                    if (f.getName().endsWith(".tmp")) {
                        Files.deleteIfExists(f.toPath());
                    }
                }

                stores = new StoreImpl(store, publisher, contentCopyPathRoot);
            }

            @Test
            void shouldPropagateStoreAccessException() throws IOException {
                when(store.getContent(any())).thenThrow(new StoreAccessException("missing property"));
                assertThatThrownBy(() -> stores.getContent(new Object()))
                    .isInstanceOf(StoreAccessException.class)
                    .hasMessage("missing property");
            }
        }
    }
}
