package org.springframework.content.encryption.fs;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.content.encryption.config.EncryptingContentStoreConfiguration;
import org.springframework.content.encryption.config.EncryptingContentStoreConfigurer;
import internal.org.springframework.content.rest.boot.autoconfigure.ContentRestAutoConfiguration;
import internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.module.mockmvc.response.MockMvcResponse;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.encryption.store.EncryptingContentStore;
import org.springframework.content.fs.config.EnableFileSystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.fs.store.FileSystemContentStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.CrudRepository;
import org.springframework.web.context.WebApplicationContext;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.UUID;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;

@SpringBootTest(classes = EncryptionIT.Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
public class EncryptionIT {

    @Autowired
    private FileRepository repo;

    @Autowired
    private FileContentStore3 store;

    @Autowired
    private java.io.File filesystemRoot;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private FsFile f;

    
    @Nested
    class ClientSideEncryptionWithFsStorageCases {
        @Nested
        class GivenContent {
            @Nested
            class Tests {
                @BeforeEach
                void setUp() {
                    RestAssuredMockMvc.webAppContextSetup(webApplicationContext);

                                    f = repo.save(new FsFile());

                    given()
                                                .contentType("text/plain")
                                                .body("Hello Client-side encryption World!")
                                                .when()
                                                .post("/fsFiles/" + f.getId() + "/content")
                                                .then()
                                                .statusCode(HttpStatus.SC_CREATED);
                }
                @Test
                void shouldBeStoredEncrypted() throws IOException {
                    Optional<FsFile> fetched = repo.findById(f.getId());
                                        assertThat(fetched.isPresent()).isTrue();
                                        f = fetched.get();

                                        String contents = IOUtils.toString(new FileInputStream(new java.io.File(filesystemRoot, f.getContentId().toString())));
                                        assertThat(contents).isNotEqualTo("Hello Client-side encryption World!");
                }
                @Test
                void shouldBeRetrievedDecrypted() {
                    MockMvcResponse response =
                                                given()
                                                .header("accept", "text/plain")
                                                .get("/fsFiles/" + f.getId() + "/content")
                                                .then()
                                                .statusCode(HttpStatus.SC_OK)
                                                .extract().response();
                                        assertThat(response.getContentType()).startsWith("text/plain");
                                        assertThat(response.asString()).isEqualTo("Hello Client-side encryption World!");
                }
                @Test
                void shouldHandleByteRangeRequests() {
                    MockMvcResponse r =
                                                given()
                                                        .header("accept", "text/plain")
                                                        .header("range", "bytes=16-27")
                                                        .get("/fsFiles/" + f.getId() + "/content")
                                                        .then()
                                                        .statusCode(HttpStatus.SC_PARTIAL_CONTENT)
                                                        .extract().response();
                                        assertThat(r.getContentType()).startsWith("text/plain");

                                        assertThat(r.asString()).isEqualTo("e encryption");
                }
            }
            @Nested
            class WhenTheContentIsUnset {
                @BeforeEach
                void setUp() {
                    RestAssuredMockMvc.webAppContextSetup(webApplicationContext);

                                    f = repo.save(new FsFile());

                    given()
                                                .contentType("text/plain")
                                                .body("Hello Client-side encryption World!")
                                                .when()
                                                .post("/fsFiles/" + f.getId() + "/content")
                                                .then()
                                                .statusCode(HttpStatus.SC_CREATED);
                }
                @Test
                void itShouldRemoveTheContentAndClearTheContentKey() {
                    f = repo.findById(f.getId()).get();
                                            String contentId = f.getContentId().toString();

                                            given()
                                                    .delete("/fsFiles/" + f.getId() + "/content")
                                                    .then()
                                                    .statusCode(HttpStatus.SC_NO_CONTENT);

                                            f = repo.findById(f.getId()).get();
                                            assertThat(f.getContentKey()).isNull();
                                            assertThat(new java.io.File(filesystemRoot, contentId).exists()).isFalse();
                }
            }
        }
    }


    @Test
    public void noop() {
    }

    @SpringBootApplication(exclude = {S3ContentAutoConfiguration.class})
    @ImportAutoConfiguration(ContentRestAutoConfiguration.class)
    @EnableJpaRepositories(considerNestedRepositories = true)
    @EnableFileSystemStores
    static class Application {
        public static void main(String[] args) {
            SpringApplication.run(Application.class, args);
        }

        @Configuration
        public static class Config {

            @Bean
            public java.io.File filesystemRoot() {
                try {
                    return Files.createTempDirectory("").toFile();
                } catch (IOException ignored) {
                }
                return null;
            }

            @Bean
            public FileSystemResourceLoader fileSystemResourceLoader() {
                return new FileSystemResourceLoader(filesystemRoot().getAbsolutePath());
            }

            @Bean
            public EncryptingContentStoreConfigurer<FileContentStore3> config() {
                return new EncryptingContentStoreConfigurer<FileContentStore3>() {
                    @Override
                    public void configure(EncryptingContentStoreConfiguration<FileContentStore3> config) {
                        config.encryptionKeyContentProperty("key");
                    }
                };
            }
        }
    }

    public interface FileRepository extends CrudRepository<FsFile, Long> {
    }

    public interface FileContentStore3 extends FileSystemContentStore<FsFile, UUID>, EncryptingContentStore<FsFile, UUID> {
    }

    @Entity
    public static class FsFile {
        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

        private String name;

        @JsonIgnore
        private byte[] contentKey;

        @ContentId
        private UUID contentId;
        @ContentLength
        private long contentLength;
        @MimeType
        private String contentMimeType;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public byte[] getContentKey() {
            return contentKey;
        }

        public void setContentKey(byte[] contentKey) {
            this.contentKey = contentKey;
        }

        public UUID getContentId() {
            return contentId;
        }

        public void setContentId(UUID contentId) {
            this.contentId = contentId;
        }

        public long getContentLength() {
            return contentLength;
        }

        public void setContentLength(long contentLength) {
            this.contentLength = contentLength;
        }

        public String getContentMimeType() {
            return contentMimeType;
        }

        public void setContentMimeType(String contentMimeType) {
            this.contentMimeType = contentMimeType;
        }
    }
}
