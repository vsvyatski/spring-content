package org.springframework.content.encryption.s3;

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
import org.springframework.vault.core.VaultTemplate;
import org.springframework.web.context.WebApplicationContext;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
public class EncryptionIT {

    private static final String BUCKET = "test-bucket";
    private static final Object mutex = new Object();

    static {
        System.setProperty("spring.content.s3.bucket", BUCKET);
    }

    @Autowired
    private FsFileRepository repo;
    @Autowired
    private S3Client client;
    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private VaultTemplate vaultTemplate;
    private FsFile f;

    @Nested
    @DisplayName("Given content")
    class Content {
        @Nested
        class Tests {
            @BeforeEach
            void setUp() {
                RestAssuredMockMvc.webAppContextSetup(webApplicationContext);

                synchronized (mutex) {
                    HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                        .bucket("test-bucket")
                        .build();

                    try {
                        client.headBucket(headBucketRequest);
                    } catch (NoSuchBucketException e) {

                        CreateBucketRequest bucketRequest = CreateBucketRequest.builder()
                            .bucket("test-bucket")
                            .build();
                        client.createBucket(bucketRequest);
                    }

                    vaultTemplate.opsForTransit().createKey("my-key");
                }

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
                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket("test-bucket")
                    .key(contentId.toString())
                    .build();

                ResponseInputStream<GetObjectResponse> resp = client.getObject(getObjectRequest);
                String contents = IOUtils.toString(resp, Charset.defaultCharset());
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
                        .header("range", "bytes=14-27")
                        .get("/files/" + f.getId() + "/content")
                        .then()
                        .statusCode(HttpStatus.SC_PARTIAL_CONTENT)
                        .extract().response();
                assertThat(r.getContentType()).startsWith("text/plain");

                assertThat(r.asString()).isEqualTo("ide encryption");

                r = given()
                    .header("accept", "text/plain")
                    .header("range", "bytes=19-27")
                    .get("/files/" + f.getId() + "/content")
                    .then()
                    .statusCode(HttpStatus.SC_PARTIAL_CONTENT)
                    .extract().response();
                assertThat(r.getContentType()).startsWith("text/plain");

                assertThat(r.asString()).isEqualTo("encryption");
            }
        }

        @Nested
        @DisplayName("When the keyring is rotated")
        class KeyringIsRotated {
            @BeforeEach
            void setUp() {
                RestAssuredMockMvc.webAppContextSetup(webApplicationContext);

                synchronized (mutex) {
                    HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                        .bucket("test-bucket")
                        .build();

                    try {
                        client.headBucket(headBucketRequest);
                    } catch (NoSuchBucketException e) {

                        CreateBucketRequest bucketRequest = CreateBucketRequest.builder()
                            .bucket("test-bucket")
                            .build();
                        client.createBucket(bucketRequest);
                    }

                    vaultTemplate.opsForTransit().createKey("my-key");
                }

                f = repo.save(new FsFile());

                given()
                    .contentType("text/plain")
                    .body("Hello Client-side encryption World!")
                    .when()
                    .post("/files/" + f.getId() + "/content")
                    .then()
                    .statusCode(HttpStatus.SC_CREATED);

                vaultTemplate.opsForTransit().rotate("my-key");
            }

            @Test
            void shouldStillRetrieveContentDecrypted() {
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

                synchronized (mutex) {
                    HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                        .bucket("test-bucket")
                        .build();

                    try {
                        client.headBucket(headBucketRequest);
                    } catch (NoSuchBucketException e) {

                        CreateBucketRequest bucketRequest = CreateBucketRequest.builder()
                            .bucket("test-bucket")
                            .build();
                        client.createBucket(bucketRequest);
                    }

                    vaultTemplate.opsForTransit().createKey("my-key");
                }

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

                HeadObjectRequest getObjectRequest = HeadObjectRequest.builder()
                    .bucket("test-bucket")
                    .key(contentId.toString())
                    .build();

                assertThatThrownBy(() -> client.headObject(getObjectRequest))
                    .isInstanceOf(NoSuchKeyException.class);
            }
        }
    }
}
