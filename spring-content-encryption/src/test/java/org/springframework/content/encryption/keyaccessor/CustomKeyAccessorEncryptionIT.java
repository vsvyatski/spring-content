package org.springframework.content.encryption.keyaccessor;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;


import java.util.Collection;

import org.springframework.content.commons.mappingcontext.ContentProperty;
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
import org.springframework.content.encryption.keys.DataEncryptionKeyAccessor;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey.UnencryptedSymmetricDataEncryptionKey;
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

@SpringBootTest(classes = CustomKeyAccessorEncryptionIT.Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
public class CustomKeyAccessorEncryptionIT {

    @Autowired
    private FileRepository repo;

    @Autowired
    private FileContentStore3 store;

    @Autowired
    private ContentEncryptionKeyRepository contentEncryptionKeyRepository;

    @Autowired
    private java.io.File filesystemRoot;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private FsFile f;

    
    @Nested
    class ClientSideEncryptionWithCustomKeyStorageCases {
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

                                        assertThat(contentEncryptionKeyRepository.findById(f.getContentId()).isPresent()).isTrue();
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
                                            assertThat(contentEncryptionKeyRepository.findById(UUID.fromString(contentId)).isEmpty()).isTrue();
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
            public EncryptingContentStoreConfigurer<FileContentStore3> config(ContentEncryptionKeyRepository encryptionKeyRepository) {
                return new EncryptingContentStoreConfigurer<FileContentStore3>() {
                    @Override
                    public void configure(EncryptingContentStoreConfiguration<FileContentStore3> config) {
                        config.dataEncryptionKeyAccessor(new EntityStorageDataEncryptionKeyAccessor<>(encryptionKeyRepository));
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

    public interface ContentEncryptionKeyRepository extends CrudRepository<ContentEncryptionKey, UUID> {
    }

    @Entity
    public static class ContentEncryptionKey {
        @Id
        private UUID contentId;

        private String algorithm;

        private byte[] encryptionKey;

        private byte[] iv;

        public UUID getContentId() {
            return contentId;
        }

        public void setContentId(UUID contentId) {
            this.contentId = contentId;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public void setAlgorithm(String algorithm) {
            this.algorithm = algorithm;
        }

        public byte[] getEncryptionKey() {
            return encryptionKey;
        }

        public void setEncryptionKey(byte[] encryptionKey) {
            this.encryptionKey = encryptionKey;
        }

        public byte[] getIv() {
            return iv;
        }

        public void setIv(byte[] iv) {
            this.iv = iv;
        }
    }

    private record EntityStorageDataEncryptionKeyAccessor<S>(
            ContentEncryptionKeyRepository contentEncryptionKeyRepository) implements DataEncryptionKeyAccessor<S, UnencryptedSymmetricDataEncryptionKey> {

        @Override
        public Collection<UnencryptedSymmetricDataEncryptionKey> findKeys(S entity, ContentProperty contentProperty) {
            var contentId = (UUID) contentProperty.getContentId(entity);
            if (contentId == null) {
                return null;
            }
            return contentEncryptionKeyRepository.findById(contentId).stream()
                    .map(encryptionKeyEntity -> new UnencryptedSymmetricDataEncryptionKey(
                            encryptionKeyEntity.getAlgorithm(),
                            encryptionKeyEntity.getEncryptionKey(),
                            encryptionKeyEntity.getIv()
                    ))
                    .toList();
        }

        @Override
        public S setKeys(S entity, ContentProperty contentProperty,
                         Collection<UnencryptedSymmetricDataEncryptionKey> dataEncryptionKeys
        ) {
            var contentId = (UUID) contentProperty.getContentId(entity);
            var maybeDataEncryptionKey = dataEncryptionKeys.stream().findFirst();

            if (maybeDataEncryptionKey.isEmpty()) {
                contentEncryptionKeyRepository.deleteById(contentId);
                return entity;
            }

            var dataEncryptionKey = maybeDataEncryptionKey.get();


            var encryptionKeyEntity = contentEncryptionKeyRepository.findById(contentId)
                    .orElseGet(() -> {
                        var contentEncryptionKey = new ContentEncryptionKey();
                        contentEncryptionKey.setContentId(contentId);
                        return contentEncryptionKey;
                    });

            encryptionKeyEntity.setAlgorithm(dataEncryptionKey.algorithm());
            encryptionKeyEntity.setEncryptionKey(dataEncryptionKey.keyData());
            encryptionKeyEntity.setIv(dataEncryptionKey.initializationVector());

            contentEncryptionKeyRepository.save(encryptionKeyEntity);

            return entity;
        }
    }
}
