package internal.org.springframework.content.commons.store.factory;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.assertj.core.api.Assertions.fail;

import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.context.ApplicationEventPublisher;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StoreImplTest {

    private StoreImpl stores;

    private ContentStore store;
    private ApplicationEventPublisher publisher;
    private Path contentCopyPathRoot;

    
    @Nested
    class StoreImplCases {
        @Nested
        class SetContentInputStream {
            @BeforeEach
            void setUp() throws IOException {
                store = mock(ContentStore.class);
                                publisher = mock(ApplicationEventPublisher.class);

                when(store.setContent(any(), any(InputStream.class))).thenReturn(new Object());

                contentCopyPathRoot = Files.createTempDirectory("storeImplTest");

                                for (File f : Objects.requireNonNull(contentCopyPathRoot.toFile().listFiles())) {
                                    if (f.getName().endsWith(".tmp")) {
                                        f.delete(); // may fail mysteriously - returns boolean you may want to check
                                    }
                                }

                                stores = new StoreImpl(store, publisher, contentCopyPathRoot);

                stores.setContent(new Object(), new ByteArrayInputStream("foo".getBytes()));
            }
            @Test
            void shouldDeleteTheContentCopyFile() {
                for (File f : Objects.requireNonNull(contentCopyPathRoot.toFile().listFiles())) {
                                        if (f.getName().endsWith(".tmp")) {
                                            fail("Found orphaned content copy path");
                                        }
                                    }
            }
        }
        @Nested
        class GetContent {
            @BeforeEach
            void setUp() throws IOException {
                store = mock(ContentStore.class);
                                publisher = mock(ApplicationEventPublisher.class);

                contentCopyPathRoot = Files.createTempDirectory("storeImplTest");

                                for (File f : Objects.requireNonNull(contentCopyPathRoot.toFile().listFiles())) {
                                    if (f.getName().endsWith(".tmp")) {
                                        f.delete(); // may fail mysteriously - returns boolean you may want to check
                                    }
                                }

                                stores = new StoreImpl(store, publisher, contentCopyPathRoot);
            }
            @Test
            void shouldPropagateStoreAccessException() throws IOException {
                when(store.getContent(any())).thenThrow(new StoreAccessException("missing property"));
                                    try {
                                        stores.getContent(new Object());
                                        fail("expected StoreAccessException");
                                    } catch (StoreAccessException expected) {
                                    }
            }
        }
    }

}
