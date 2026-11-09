package it.rest.embeddedid;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.hateoas.autoconfigure.HypermediaAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.fs.config.EnableFileSystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.fs.store.FileSystemContentStore;
import org.springframework.content.rest.config.ContentRestConfigurer;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {EmbeddedIdTest.Application.class},
        webEnvironment = WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration(exclude = {HypermediaAutoConfiguration.class, SecurityAutoConfiguration.class})
@ExtendWith(SpringExtension.class)
public class EmbeddedIdTest {

    @LocalServerPort
    private int serverPort;

    @Autowired
    private TestEntityRepository repo;

    @Autowired
    private TestEntityContentRepository store;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @SuppressWarnings("unused") // classpath / store component scan
    public interface TestEntityRepository extends JpaRepository<TestEntity, TestEntityId> {
    }

    @SuppressWarnings("unused") // classpath / store component scan
    public interface TestEntityContentRepository extends FileSystemContentStore<TestEntity, TestEntityId> {
    }

    @SpringBootApplication
    @EnableJpaRepositories(considerNestedRepositories = true, basePackages = {"it.rest.embeddedid"})
    @EnableFileSystemStores(basePackages = "it.rest.embeddedid")
    @Import({RestConfiguration.class})
    public static class Application {

        public static void main(String[] args) {
            SpringApplication.run(Application.class, args);
        }

        @Bean
        public FileSystemResourceLoader filesystemRoot() throws IOException {
            return new FileSystemResourceLoader(Files.createTempDirectory("").toFile().getAbsolutePath());
        }

        @Bean
        public ContentRestConfigurer configureConversionService() {
            return config -> config.converters().addConverter((Converter<String, TestEntityId>) source -> {
                String[] segments = source.split("_");
                return new TestEntityId(segments[0], segments[1]);
            });
        }
    }

    @Entity
    public static class TestEntity {

        @EmbeddedId
        private TestEntityId id = new TestEntityId();

        @ContentId
        @Column(name = "content_id")
        private String contentId;

        @ContentLength
        @Column(name = "content_length")
        private long contentLength;

        @MimeType
        @Column(name = "mime_type")
        private String mimeType = "text/plain";

        public TestEntityId getId() {
            return id;
        }

        public void setId(TestEntityId id) {
            this.id = id;
        }

        public String getContentId() {
            return contentId;
        }

        public void setContentId(String contentId) {
            this.contentId = contentId;
        }

        public long getContentLength() {
            return contentLength;
        }

        public void setContentLength(long contentLength) {
            this.contentLength = contentLength;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        @Override
        public String toString() {
            return "TestEntity(id=" + id + ", contentId=" + contentId + ", contentLength=" + contentLength + ", mimeType=" + mimeType + ")";
        }
    }

    public static class TestEntityId implements Serializable {

        @Serial
        private static final long serialVersionUID = -1710555467685181030L;

        private final String first;

        private final String last;

        public TestEntityId() {
            this.first = UUID.randomUUID().toString();
            this.last = UUID.randomUUID().toString();
        }

        public TestEntityId(String mediumId, String fileId) {
            this.first = mediumId;
            this.last = fileId;
        }

        @Override
        public String toString() {
            return first + "_" + last;
        }
    }

    @Nested
    class EmbeddedIdCases {
        @BeforeEach
        void setUp() {
            mvc = MockMvcBuilders.webAppContextSetup(context).build();
        }

        @Test
        void shouldHaveAContentHandlerMappingBean() throws Exception {
            String content = "this is some content";

            TestEntity entity = repo.save(new TestEntity());
            entity = store.setContent(entity, new ByteArrayInputStream(content.getBytes()));
            entity = repo.save(entity);

            MockHttpServletResponse response =
                    mvc.perform(
                                    get("/testEntities/" + entity.getId()).
                                            accept("text/plain")).
                            andExpect(status().isOk()).
                            andReturn().getResponse();

            assertThat(response).isNotNull();
            assertThat(response.getContentAsString()).isEqualTo(content);
        }
    }
}
