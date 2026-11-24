package org.springframework.content.encryption.fs;

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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.content.test.support.encryption.FsFile;
import org.springframework.content.test.support.encryption.FsFileRepository;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.context.WebApplicationContext;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
public class EncryptionIT {

    @Autowired
    private FsFileRepository repo;

    @Autowired
    private java.io.File filesystemRoot;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private FsFile f;

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

                UUID contentId = Objects.requireNonNull(f.getContentId());
                String contents;
                try (InputStream in = new FileInputStream(new java.io.File(filesystemRoot, contentId.toString()))) {
                    contents = IOUtils.toString(in, StandardCharsets.UTF_8);
                }
                assertThat(contents).isNotEqualTo("Hello Client-side encryption World!");
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

            @Test
            void shouldHandleByteRangeRequests() {
                MockMvcResponse r =
                    given()
                        .header("accept", "text/plain")
                        .header("range", "bytes=16-27")
                        .get("/files/" + f.getId() + "/content")
                        .then()
                        .statusCode(HttpStatus.SC_PARTIAL_CONTENT)
                        .extract().response();
                assertThat(r.getContentType()).startsWith("text/plain");

                assertThat(r.asString()).isEqualTo("e encryption");
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
                UUID contentId = Objects.requireNonNull(f.getContentId());

                given()
                    .delete("/files/" + f.getId() + "/content")
                    .then()
                    .statusCode(HttpStatus.SC_NO_CONTENT);

                f = repo.findById(f.getId()).orElseThrow();
                assertThat(f.getContentKey()).isNull();
                assertThat(new java.io.File(filesystemRoot, contentId.toString()).exists()).isFalse();
            }
        }
    }
}
