package it.events;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.HandleBeforeSetContent;
import org.springframework.content.commons.annotations.StoreEventHandler;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.events.BeforeSetContentEvent;
import org.springframework.content.fs.config.EnableFileSystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(Ginkgo4jSpringRunner.class)
@ContextConfiguration(classes = {BeforeSetEventIT.TestConfig.class})
public class BeforeSetEventIT {

    static TestConfig.ExampleAnnotatedEventHandler ev;

    @Autowired
    private TestEntityContentStore store;

    {
        Describe("BeforeSetEvent InputStream Access", () -> {

            Context("when the content input stream is consumed by a beforeSetEvent", () -> {

                It("should still set the content in the store", () -> {
                    TestEntity te = new TestEntity();
                    ByteArrayInputStream bais = new ByteArrayInputStream("Still here!".getBytes());
                    te = store.setContent(te, bais);
                    IOUtils.closeQuietly(bais);
                    try (InputStream foo = store.getContent(te)) {
                        assertEquals("Still here!", IOUtils.toString(foo, Charset.defaultCharset()));
                    }

                    verify(ev, times(1));
                });
            });
        });
    }

    @Configuration
    @EnableFileSystemStores
    public static class TestConfig {

        @Bean
        FileSystemResourceLoader fs() throws IOException {
            return new FileSystemResourceLoader(Files.createTempDirectory("").toFile().getAbsolutePath());
        }

        @Bean
        public ExampleAnnotatedEventHandler eventHandler() {
            ev = spy(new ExampleAnnotatedEventHandler());
            return ev;
        }

        @StoreEventHandler
        public static class ExampleAnnotatedEventHandler {

            @HandleBeforeSetContent
            public void handleBeforeSetContentEvent(BeforeSetContentEvent event) throws IOException {
                InputStream is = event.getInputStream();
                IOUtils.copy(is, new ByteArrayOutputStream());
            }
        }
    }

    public class TestEntity {
        @ContentId
        private String contentId;

        public TestEntity() {
        }

        public String getContentId() {
            return contentId;
        }

        public void setContentId(String contentId) {
            this.contentId = contentId;
        }
    }

    public interface TestEntityContentStore extends ContentStore<TestEntity, String> {
    }

    @Test
    public void noop() {
    }
}
