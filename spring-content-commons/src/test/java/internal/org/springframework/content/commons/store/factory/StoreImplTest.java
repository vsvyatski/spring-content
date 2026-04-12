package internal.org.springframework.content.commons.store.factory;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.junit.runner.RunWith;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.context.ApplicationEventPublisher;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Ginkgo4jRunner.class)
public class StoreImplTest {

    private StoreImpl stores;

    private ContentStore store;
    private ApplicationEventPublisher publisher;
    private Path contentCopyPathRoot;

    {
        Describe("StoreImpl", () -> {

            BeforeEach(() -> {
                store = mock(ContentStore.class);
                publisher = mock(ApplicationEventPublisher.class);

            });
            JustBeforeEach(() -> {
                contentCopyPathRoot = Files.createTempDirectory("storeImplTest");

                for (File f : Objects.requireNonNull(contentCopyPathRoot.toFile().listFiles())) {
                    if (f.getName().endsWith(".tmp")) {
                        f.delete(); // may fail mysteriously - returns boolean you may want to check
                    }
                }

                stores = new StoreImpl(ContentStore.class, store, publisher, contentCopyPathRoot);
            });

            Context("#setContent - inputStream", () -> {

                BeforeEach(() -> when(store.setContent(any(), any(InputStream.class))).thenReturn(new Object()));

                JustBeforeEach(() -> {
                    stores.setContent(new Object(), new ByteArrayInputStream("foo".getBytes()));
                });

                It("should delete the content copy file", () -> {
                    for (File f : Objects.requireNonNull(contentCopyPathRoot.toFile().listFiles())) {
                        if (f.getName().endsWith(".tmp")) {
                            fail("Found orphaned content copy path");
                        }
                    }
                });
            });
        });
    }
}
