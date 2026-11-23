package org.springframework.content.encryption.keyaccessor;

import internal.org.springframework.content.rest.boot.autoconfigure.ContentRestAutoConfiguration;
import internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.module.mockmvc.response.MockMvcResponse;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.encryption.config.EncryptingContentStoreConfigurer;
import org.springframework.content.encryption.fs.EncryptedFsFileStore;
import org.springframework.content.encryption.keys.DataEncryptionKeyAccessor;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey.UnencryptedSymmetricDataEncryptionKey;
import org.springframework.content.fs.config.EnableFileSystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.test.support.TestSupportPackages;
import org.springframework.content.test.support.encryption.ContentEncryptionKey;
import org.springframework.content.test.support.encryption.ContentEncryptionKeyRepository;
import org.springframework.content.test.support.encryption.FsFile;
import org.springframework.content.test.support.encryption.FsFileRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.context.WebApplicationContext;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CustomKeyAccessorEncryptionIT.Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
public class CustomKeyAccessorEncryptionIT {

    @Autowired
    private FsFileRepository repo;

    @Autowired
    private ContentEncryptionKeyRepository contentEncryptionKeyRepository;

    @Autowired
    private java.io.File filesystemRoot;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private FsFile f;

    @SpringBootApplication(exclude = {S3ContentAutoConfiguration.class})
    @ImportAutoConfiguration(ContentRestAutoConfiguration.class)
    @EnableJpaRepositories(basePackages = TestSupportPackages.ENCRYPTION, considerNestedRepositories = true)
    @EnableFileSystemStores(basePackages = "org.springframework.content.encryption.fs")
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
            public EncryptingContentStoreConfigurer<EncryptedFsFileStore> config(
                ContentEncryptionKeyRepository encryptionKeyRepository) {
                return config -> config.dataEncryptionKeyAccessor(
                    new EntityStorageDataEncryptionKeyAccessor<>(encryptionKeyRepository));
            }
        }
    }

    private record EntityStorageDataEncryptionKeyAccessor<S>(
        ContentEncryptionKeyRepository contentEncryptionKeyRepository)
        implements DataEncryptionKeyAccessor<S, UnencryptedSymmetricDataEncryptionKey> {

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

    @Nested
   class ClientSideEncryptionWithCustomKeyStorageCases {
        @Nested
        @DisplayName("Given content")
        class Content {
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
                        .post("/files/" + f.getId() + "/content")
                        .then()
                        .statusCode(HttpStatus.SC_CREATED);
                }

                @Test
                void shouldBeStoredEncrypted() throws IOException {
                    Optional<FsFile> fetched = repo.findById(f.getId());
                    assertThat(fetched.isPresent()).isTrue();
                    f = fetched.get();

                    String contents;
                    try (InputStream in = new FileInputStream(new java.io.File(filesystemRoot, f.getContentId().toString()))) {
                        contents = IOUtils.toString(in, StandardCharsets.UTF_8);
                    }
                    assertThat(contents).isNotEqualTo("Hello Client-side encryption World!");

                    assertThat(contentEncryptionKeyRepository.findById(f.getContentId()).isPresent()).isTrue();
                }

                @Test
                void shouldBeRetrievedDecrypted() {
                    MockMvcResponse response =
                        given()
                            .header("accept", "text/plain")
                            .get("/files/" + f.getId() + "/content")
                            .then()
                            .statusCode(HttpStatus.SC_OK)
                            .extract().response();
                    assertThat(response.getContentType()).startsWith("text/plain");
                    assertThat(response.asString()).isEqualTo("Hello Client-side encryption World!");
                }
            }

            @Nested

            @DisplayName("When the content is unset")

            class ContentIsUnset {
                @BeforeEach
                void setUp() {
                    RestAssuredMockMvc.webAppContextSetup(webApplicationContext);

                    f = repo.save(new FsFile());

                    given()
                        .contentType("text/plain")
                        .body("Hello Client-side encryption World!")
                        .when()
                        .post("/files/" + f.getId() + "/content")
                        .then()
                        .statusCode(HttpStatus.SC_CREATED);
                }

                @Test
                void itShouldRemoveTheContentAndClearTheContentKey() {
                    f = repo.findById(f.getId()).orElseThrow();
                    String contentId = f.getContentId().toString();

                    given()
                        .delete("/files/" + f.getId() + "/content")
                        .then()
                        .statusCode(HttpStatus.SC_NO_CONTENT);

                    f = repo.findById(f.getId()).orElseThrow();
                    assertThat(contentEncryptionKeyRepository.findById(UUID.fromString(contentId)).isEmpty()).isTrue();
                    assertThat(new java.io.File(filesystemRoot, contentId).exists()).isFalse();
                }
            }
        }
    }
}
