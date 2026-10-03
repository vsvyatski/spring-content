package it.events;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.HandleBeforeSetContent;
import org.springframework.content.commons.annotations.StoreEventHandler;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.events.BeforeSetContentEvent;
import org.springframework.content.fs.config.EnableFileSystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {BeforeSetEventIT.TestConfig.class})
@ExtendWith(SpringExtension.class)
public class BeforeSetEventIT {

    static TestConfig.ExampleAnnotatedEventHandler ev;

    @Autowired
    private TestEntityContentStore store;

    
    @Nested
    class BeforeSetEventInputStreamAccessCases {
        @Nested
        class WhenTheContentInputStreamIsConsumedByABeforeSetEvent {
            @Test
            void shouldStillSetTheContentInTheStore() throws IOException {
                TestEntity te = new TestEntity();
                                    ByteArrayInputStream bais = new ByteArrayInputStream("Still here!".getBytes());
                                    te = store.setContent(te, bais);
                                    IOUtils.closeQuietly(bais);
                                    try (InputStream foo = store.getContent(te)) {
                                        assertThat("Still here!").isEqualTo(IOUtils.toString(foo, Charset.defaultCharset()));
                                    }

                                    verify(ev, times(1));
            }
        }
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

    public static class TestEntity {
        @ContentId
        private String contentId;

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
