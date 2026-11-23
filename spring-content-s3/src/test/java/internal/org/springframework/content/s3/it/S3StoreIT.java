package internal.org.springframework.content.s3.it;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.io.RangeableResource;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.SetContentParams;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.commons.store.UnsetContentParams;
import org.springframework.content.s3.config.EnableS3Stores;
import org.springframework.content.test.support.TestSupportData;
import org.springframework.content.test.support.TestSupportPackages;
import org.springframework.content.test.support.jpa.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest()
@ExtendWith(SpringExtension.class)
public class S3StoreIT {

    private static final String BUCKET = "test-bucket";
    private static final Object mutex = new Object();

    static {
        System.setProperty("spring.content.s3.bucket", BUCKET);
    }

    @Autowired
    private DualContentJpaLongIdMimeRepository repo;
    @Autowired
    private DualContentJpaLongIdMimeStore store;
    @Autowired
    private S3Client client;
    private String resourceLocation;
    private Resource genericResource;
    private DualContentJpaLongIdMimeEntity entity;
    private Exception e;
    // Shared id tests
    @Autowired
    private SharedIdJakartaJpaRepository sharedIdRepository;
    @Autowired
    private SharedIdJakartaJpaStore sharedIdStore;
    // Embedded content tests
    @Autowired
    private EmbeddedContentJpaStringIdRepository embeddedRepo;
    @Autowired
    private EmbeddedContentJpaStringIdStore embeddedStore;


    private void ensureTestBucket() throws InterruptedException {
        synchronized (mutex) {
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                .bucket(BUCKET)
                .build();
            try {
                client.headBucket(headBucketRequest);
            } catch (NoSuchBucketException ignored) {
                client.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
                boolean found = false;
                while (!found) {
                    headBucketRequest = HeadBucketRequest.builder().bucket(BUCKET).build();
                    try {
                        client.headBucket(headBucketRequest);
                        found = true;
                    } catch (NoSuchBucketException ignoredPoll) {
                        // LocalStack: bucket not visible yet
                    }
                    System.out.println("sleeping...");
                    Thread.sleep(100);
                }
            }
        }
    }

    @SpringBootApplication(scanBasePackages = {"internal.org.springframework.content.s3.it", TestSupportPackages.JPA})
    @EntityScan(basePackages = TestSupportPackages.JPA)
    @EnableJpaRepositories(basePackages = {TestSupportPackages.JPA, "internal.org.springframework.content.s3.it"}, considerNestedRepositories = true)
    @EnableS3Stores(basePackages = {TestSupportPackages.JPA, "internal.org.springframework.content.s3.it"})
    static class Application {
        public static void main(String[] args) {
            SpringApplication.run(Application.class, args);
        }

        @Configuration
        public static class Config {

            @Bean
            public S3Client amazonS3() throws URISyntaxException {
                return LocalStack.getAmazonS3Client();
            }
        }
    }

    @Nested
    class S3StorageCases {
        @Nested
        class StoreCases {
            @Nested
            class GetResource {
                @Nested
                class Tests {
                    @BeforeEach
                    void setUp() throws InterruptedException {
                        ensureTestBucket();

                        resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                        genericResource = store.getResource(resourceLocation);
                    }

                    @AfterEach
                    void tearDown() throws IOException {
                        ((DeletableResource) genericResource).delete();
                    }

                    @Test
                    void shouldGetResource() {
                        assertThat(genericResource).isInstanceOf(Resource.class);
                    }

                    @Test
                    void shouldNotExist() {
                        assertThat(genericResource.exists()).isFalse();
                    }

                    @Test
                    void shouldBeARangeableResource() {
                        assertThat(genericResource).isInstanceOf(RangeableResource.class);
                    }
                }

                @Nested
                @DisplayName("Given content is added to that resource")
                class ContentIsAddedToThatResource {
                    @Nested
                    class Tests {
                        @BeforeEach
                        void setUp() throws IOException, InterruptedException {
                            ensureTestBucket();

                            resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                            genericResource = store.getResource(resourceLocation);

                            try (InputStream is = TestSupportData.sampleTextStream()) {
                                try (OutputStream os = ((WritableResource) genericResource).getOutputStream()) {
                                    IOUtils.copy(is, os);
                                }
                            }
                        }

                        @AfterEach
                        void tearDown() throws IOException {
                            ((DeletableResource) genericResource).delete();
                        }

                        @Test
                        void shouldStoreThatContent() throws IOException {
                            assertThat(genericResource.exists()).isTrue();

                            try (InputStream expected = TestSupportData.sampleTextStream();
                                 InputStream actual = genericResource.getInputStream()) {
                                assertThat(IOUtils.contentEquals(expected, actual)).isTrue();
                            }
                        }
                    }

                    @Nested
                    @DisplayName("Given that resource is then updated")
                    class ResourceIsThenUpdated {
                        @BeforeEach
                        void setUp() throws IOException, InterruptedException {
                            ensureTestBucket();

                            resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                            genericResource = store.getResource(resourceLocation);

                            try (InputStream is = TestSupportData.sampleTextStream()) {
                                try (OutputStream os = ((WritableResource) genericResource).getOutputStream()) {
                                    IOUtils.copy(is, os);
                                }
                            }

                            try (InputStream is = TestSupportData.sampleUpdatedTextStream()) {
                                try (OutputStream os = ((WritableResource) genericResource).getOutputStream()) {
                                    IOUtils.copy(is, os);
                                }
                            }
                        }

                        @AfterEach
                        void tearDown() throws IOException {
                            ((DeletableResource) genericResource).delete();
                        }

                        @Test
                        void shouldStoreThatUpdatedContent() throws IOException {
                            assertThat(genericResource.exists()).isTrue();

                            try (InputStream expected = TestSupportData.sampleUpdatedTextStream()) {
                                try (InputStream actual = genericResource.getInputStream()) {
                                    assertThat(IOUtils.contentEquals(expected, actual)).isTrue();
                                }
                            }
                        }
                    }

                    @Nested
                    @DisplayName("Given a byte range is requested")
                    class ByteRangeIsRequested {
                        @BeforeEach
                        void setUp() throws IOException, InterruptedException {
                            ensureTestBucket();

                            resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                            genericResource = store.getResource(resourceLocation);

                            try (InputStream is = TestSupportData.sampleTextStream()) {
                                try (OutputStream os = ((WritableResource) genericResource).getOutputStream()) {
                                    IOUtils.copy(is, os);
                                }
                            }
                        }

                        @AfterEach
                        void tearDown() throws IOException {
                            ((DeletableResource) genericResource).delete();
                        }

                        @Test
                        void shouldReturnAPartialContentInputStreamAndThePartialContent() throws IOException {
                            ((RangeableResource) genericResource).setRange("bytes=6-19");

                            var expectedBytes = TestSupportData.sampleTextBytes();
                            Arrays.fill(expectedBytes, 0, 6, (byte) 0); // First 5 bytes are absent
                            Arrays.fill(expectedBytes, 20, expectedBytes.length, (byte) 0); // Bytes after position 19 are absent

                            try (InputStream actual = genericResource.getInputStream()) {
                                var actualBytes = actual.readAllBytes();
                                assertThat(actualBytes).isEqualTo(expectedBytes);
                            }
                        }
                    }

                    @Nested
                    @DisplayName("Given that resource is then deleted")
                    class ResourceIsThenDeleted {
                        @BeforeEach
                        void setUp() throws IOException, InterruptedException {
                            ensureTestBucket();

                            resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                            genericResource = store.getResource(resourceLocation);

                            try (InputStream is = TestSupportData.sampleTextStream()) {
                                try (OutputStream os = ((WritableResource) genericResource).getOutputStream()) {
                                    IOUtils.copy(is, os);
                                }
                            }

                            try {
                                ((DeletableResource) genericResource).delete();
                            } catch (Exception e) {
                                S3StoreIT.this.e = e;
                            }
                        }

                        @AfterEach
                        void tearDown() throws IOException {
                            ((DeletableResource) genericResource).delete();
                        }

                        @Test
                        void shouldNotExist() {
                            assertThat(e).isNull();
                        }
                    }
                }
            }
        }

        @Nested
        class AssociativeStoreCases {
            @Nested
            @DisplayName("Given a new entity")
            class NewEntity {
                @Nested
                class Tests {
                    @BeforeEach
                    void setUp() throws InterruptedException {
                        ensureTestBucket();

                        resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                        entity = new DualContentJpaLongIdMimeEntity();
                        entity = repo.save(entity);
                    }

                    @Test
                    void shouldNotHaveAnAssociatedResource() {
                        assertThat(entity.getContentId()).isNull();
                        assertThat(store.getResource(entity)).isNull();
                    }
                }

                @Nested
                @DisplayName("Given a resource")
                class AssociatedResource {
                    @Nested
                    @DisplayName("When the resource is associated")
                    class ResourceIsAssociated {
                        @Nested
                        class Tests {
                            @BeforeEach
                            void setUp() throws InterruptedException {
                                ensureTestBucket();

                                resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                                entity = new DualContentJpaLongIdMimeEntity();
                                entity = repo.save(entity);

                                genericResource = store.getResource(resourceLocation);

                                store.associate(entity, resourceLocation);
                                store.associate(entity, PropertyPath.from("rendition"), resourceLocation);
                            }

                            @Test
                            void shouldBeRecordedAsSuchOnTheEntitySContentId() {
                                assertThat(entity.getContentId()).isEqualTo(resourceLocation);
                                assertThat(entity.getRenditionId()).isEqualTo(resourceLocation);
                            }
                        }

                        @Nested
                        @DisplayName("When the resource is unassociated")
                        class ResourceIsUnassociated {
                            @BeforeEach
                            void setUp() throws InterruptedException {
                                ensureTestBucket();

                                resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                                entity = new DualContentJpaLongIdMimeEntity();
                                entity = repo.save(entity);

                                genericResource = store.getResource(resourceLocation);

                                store.associate(entity, resourceLocation);
                                store.associate(entity, PropertyPath.from("rendition"), resourceLocation);

                                store.unassociate(entity);
                                store.unassociate(entity, PropertyPath.from("rendition"));
                            }

                            @Test
                            void shouldResetTheEntitySContentId() {
                                assertThat(entity.getContentId()).isNull();
                                assertThat(entity.getRenditionId()).isNull();
                            }
                        }

                        @Nested
                        @DisplayName("When a invalid property path is used to associate a resource")
                        class InvalidPropertyPathIsUsedToAssociateResource {
                            @BeforeEach
                            void setUp() throws InterruptedException {
                                ensureTestBucket();

                                resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                                entity = new DualContentJpaLongIdMimeEntity();
                                entity = repo.save(entity);

                                genericResource = store.getResource(resourceLocation);

                                store.associate(entity, resourceLocation);
                                store.associate(entity, PropertyPath.from("rendition"), resourceLocation);
                            }

                            @Test
                            void shouldThrowAnError() {
                                try {
                                    store.associate(entity, PropertyPath.from("does.not.exist"), resourceLocation);
                                } catch (Exception sae) {
                                    S3StoreIT.this.e = sae;
                                }
                                assertThat(e).isInstanceOf(StoreAccessException.class);
                            }
                        }

                        @Nested
                        @DisplayName("When a invalid property path is used to load a resource")
                        class InvalidPropertyPathIsUsedToLoadResource {
                            @BeforeEach
                            void setUp() throws InterruptedException {
                                ensureTestBucket();

                                resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                                entity = new DualContentJpaLongIdMimeEntity();
                                entity = repo.save(entity);

                                genericResource = store.getResource(resourceLocation);

                                store.associate(entity, resourceLocation);
                                store.associate(entity, PropertyPath.from("rendition"), resourceLocation);
                            }

                            @Test
                            void shouldThrowAnError() {
                                try {
                                    store.getResource(entity, PropertyPath.from("does.not.exist"));
                                } catch (Exception sae) {
                                    S3StoreIT.this.e = sae;
                                }
                                assertThat(e).isInstanceOf(StoreAccessException.class);
                            }
                        }

                        @Nested
                        @DisplayName("When a invalid property path is used to unassociate a resource")
                        class InvalidPropertyPathIsUsedToUnassociateResource {
                            @BeforeEach
                            void setUp() throws InterruptedException {
                                ensureTestBucket();

                                resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                                entity = new DualContentJpaLongIdMimeEntity();
                                entity = repo.save(entity);

                                genericResource = store.getResource(resourceLocation);

                                store.associate(entity, resourceLocation);
                                store.associate(entity, PropertyPath.from("rendition"), resourceLocation);
                            }

                            @Test
                            void shouldThrowAnError() {
                                try {
                                    store.unassociate(entity, PropertyPath.from("does.not.exist"));
                                } catch (Exception sae) {
                                    S3StoreIT.this.e = sae;
                                }
                                assertThat(e).isInstanceOf(StoreAccessException.class);
                            }
                        }
                    }
                }
            }
        }

        @Nested
        class ContentStoreCases {
            @Nested
            class Tests {
                @BeforeEach
                void setUp() throws InterruptedException {
                    ensureTestBucket();

                    resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                    entity = new DualContentJpaLongIdMimeEntity();
                    entity.setContentType("text/plain");
                    entity.setRenditionType("text/html");
                    entity = repo.save(entity);

                    store.setContent(entity, TestSupportData.sampleTextStream());
                    store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.sampleHtmlRenditionStream());
                }

                @Test
                void shouldBeAbleToStoreNewContent() throws IOException {
                    // content
                    try (InputStream content = store.getContent(entity)) {
                        assertThat(IOUtils.contentEquals(TestSupportData.sampleTextStream(), content)).isTrue();
                    }

                    //rendition
                    try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                        assertThat(IOUtils.contentEquals(TestSupportData.sampleHtmlRenditionStream(), content)).isTrue();
                    }
                }

                @Test
                void shouldHaveContentMetadata() {
                    // content
                    assertThat(entity.getContentId()).isNotNull();
                    assertThat(entity.getContentId().trim().length()).isGreaterThan(0);
                    assertThat(entity.getContentLen()).isEqualTo(Long.valueOf(27L));

                    //rendition
                    assertThat(entity.getRenditionId()).isNotNull();
                    assertThat(entity.getRenditionId().trim().length()).isGreaterThan(0);
                    assertThat(entity.getRenditionLen()).isEqualTo(40L);

                    assertThat(entity.getContentType()).isEqualTo("text/plain");
                    assertThat(entity.getRenditionType()).isEqualTo("text/html");

                    HeadObjectResponse contentHead = client.headObject(
                        HeadObjectRequest.builder().bucket(BUCKET).key(entity.getContentId()).build());
                    assertThat(contentHead.contentType()).isEqualTo("text/plain");

                    HeadObjectResponse renditionHead = client.headObject(
                        HeadObjectRequest.builder().bucket(BUCKET).key(entity.getRenditionId()).build());
                    assertThat(renditionHead.contentType()).isEqualTo("text/html");
                }
            }

            @Nested
            @DisplayName("When content is updated")
            class ContentIsUpdated {
                @BeforeEach
                void setUp() throws InterruptedException {
                    ensureTestBucket();

                    resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                    entity = new DualContentJpaLongIdMimeEntity();
                    entity.setContentType("text/plain");
                    entity.setRenditionType("text/html");
                    entity = repo.save(entity);

                    store.setContent(entity, TestSupportData.sampleTextStream());
                    store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.sampleHtmlRenditionStream());

                    store.setContent(entity, TestSupportData.sampleUpdatedTextStream());
                    store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.sampleUpdatedHtmlRenditionStream());
                    entity = repo.save(entity);
                }

                @Test
                void shouldHaveTheUpdatedContent() throws IOException {
                    //content
                    try (InputStream content = store.getContent(entity)) {
                        assertThat(IOUtils.contentEquals(TestSupportData.sampleUpdatedTextStream(), content)).isTrue();
                    }

                    //rendition
                    try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                        assertThat(IOUtils.contentEquals(TestSupportData.sampleUpdatedHtmlRenditionStream(), content)).isTrue();
                    }
                }
            }

            @Nested
            @DisplayName("When content is updated with shorter content")
            class ContentIsUpdatedWithShorterContent {
                @BeforeEach
                void setUp() throws InterruptedException {
                    ensureTestBucket();

                    resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                    entity = new DualContentJpaLongIdMimeEntity();
                    entity.setContentType("text/plain");
                    entity.setRenditionType("text/html");
                    entity = repo.save(entity);

                    store.setContent(entity, TestSupportData.sampleTextStream());
                    store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.sampleHtmlRenditionStream());

                    store.setContent(entity, TestSupportData.samplePartialUpdateStream());
                    store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.samplePartialUpdateHtmlRenditionStream());
                    entity = repo.save(entity);
                }

                @Test
                void shouldStoreOnlyTheNewContent() throws IOException {
                    //content
                    try (InputStream content = store.getContent(entity)) {
                        assertThat(IOUtils.contentEquals(TestSupportData.samplePartialUpdateStream(), content)).isTrue();
                    }

                    //rendition
                    try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                        assertThat(IOUtils.contentEquals(TestSupportData.samplePartialUpdateHtmlRenditionStream(), content)).isTrue();
                    }
                }
            }

            @Nested
            @DisplayName("When content is updated and not overwritten")
            class ContentIsUpdatedAndNotOverwritten {
                @BeforeEach
                void setUp() throws InterruptedException {
                    ensureTestBucket();

                    resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                    entity = new DualContentJpaLongIdMimeEntity();
                    entity.setContentType("text/plain");
                    entity.setRenditionType("text/html");
                    entity = repo.save(entity);

                    store.setContent(entity, TestSupportData.sampleTextStream());
                    store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.sampleHtmlRenditionStream());
                }

                @Test
                void shouldHaveTheUpdatedContent() throws IOException {
                    String contentId = entity.getContentId();
                    client.headObject(HeadObjectRequest.builder().bucket(BUCKET).key(contentId).build());

                    store.setContent(entity, PropertyPath.from("content"), TestSupportData.sampleUpdatedTextStream(), new SetContentParams(-1, true, SetContentParams.ContentDisposition.CreateNew));
                    entity = repo.save(entity);

                    try (InputStream content = store.getContent(entity)) {
                        assertThat(IOUtils.contentEquals(TestSupportData.sampleUpdatedTextStream(), content)).isTrue();
                    }

                    assertThat(entity.getContentId()).isNotEqualTo(contentId);
                    client.headObject(HeadObjectRequest.builder().bucket(BUCKET).key(entity.getContentId()).build());
                }
            }

            @Nested
            @DisplayName("When content is unset")
            class ContentIsUnset {
                @BeforeEach
                void setUp() throws InterruptedException {
                    ensureTestBucket();

                    resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                    entity = new DualContentJpaLongIdMimeEntity();
                    entity.setContentType("text/plain");
                    entity.setRenditionType("text/html");
                    entity = repo.save(entity);

                    store.setContent(entity, TestSupportData.sampleTextStream());
                    store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.sampleHtmlRenditionStream());

                    resourceLocation = entity.getContentId();
                    assertThat(resourceLocation).isNotNull();
                    entity = store.unsetContent(entity);
                    entity = store.unsetContent(entity, PropertyPath.from("rendition"));
                    entity = repo.save(entity);
                }

                @Test
                void shouldHaveNoContent() throws IOException {
                    //content
                    try (InputStream content = store.getContent(entity)) {
                        assertThat(content).isNull();
                    }

                    assertThat(entity.getContentId()).isNull();
                    assertThat(entity.getContentLen()).isNull();

                    assertThatThrownBy(() -> client.headObject(
                        HeadObjectRequest.builder().bucket(BUCKET).key(resourceLocation).build()))
                        .isInstanceOf(NoSuchKeyException.class);

                    //rendition
                    try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                        assertThat(content).isNull();
                    }

                    assertThat(entity.getRenditionId()).isNull();
                    assertThat(entity.getRenditionLen()).isNull();
                }
            }

            @Nested
            @DisplayName("When content is unset but kept")
            class ContentIsUnsetButKept {
                @BeforeEach
                void setUp() throws InterruptedException {
                    ensureTestBucket();

                    resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                    entity = new DualContentJpaLongIdMimeEntity();
                    entity.setContentType("text/plain");
                    entity.setRenditionType("text/html");
                    entity = repo.save(entity);

                    store.setContent(entity, TestSupportData.sampleTextStream());
                    store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.sampleHtmlRenditionStream());

                    resourceLocation = entity.getContentId();
                    assertThat(resourceLocation).isNotNull();
                    entity = store.unsetContent(entity, PropertyPath.from("content"), new UnsetContentParams(UnsetContentParams.Disposition.Keep));
                    entity = repo.save(entity);
                }

                @Test
                void shouldHaveNoContent() throws IOException {
                    //content
                    try (InputStream content = store.getContent(entity)) {
                        assertThat(content).isNull();
                    }

                    assertThat(entity.getContentId()).isNull();
                    assertThat(entity.getContentLen()).isNull();

                    client.headObject(HeadObjectRequest.builder().bucket(BUCKET).key(resourceLocation).build());
                }
            }

            @Nested
            @DisplayName("When an invalid property path is used to set content")
            class InvalidPropertyPathIsUsedToSetContent {
                @BeforeEach
                void setUp() throws InterruptedException {
                    ensureTestBucket();

                    resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                    entity = new DualContentJpaLongIdMimeEntity();
                    entity.setContentType("text/plain");
                    entity.setRenditionType("text/html");
                    entity = repo.save(entity);

                    store.setContent(entity, TestSupportData.sampleTextStream());
                    store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.sampleHtmlRenditionStream());
                }

                @Test
                void shouldThrowAnError() {
                    try {
                        store.setContent(entity, PropertyPath.from("does.not.exist"), new ByteArrayInputStream("foo".getBytes()));
                    } catch (Exception sae) {
                        S3StoreIT.this.e = sae;
                    }
                    assertThat(e).isInstanceOf(StoreAccessException.class);
                }
            }

            @Nested
            @DisplayName("When an invalid property path is used to GET content")
            class InvalidPropertyPathIsUsedToGetContent {
                @BeforeEach
                void setUp() throws InterruptedException {
                    ensureTestBucket();

                    resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                    entity = new DualContentJpaLongIdMimeEntity();
                    entity.setContentType("text/plain");
                    entity.setRenditionType("text/html");
                    entity = repo.save(entity);

                    store.setContent(entity, TestSupportData.sampleTextStream());
                    store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.sampleHtmlRenditionStream());
                }

                @Test
                void shouldThrowAnError() {
                    try {
                        store.getContent(entity, PropertyPath.from("does.not.exist"));
                    } catch (Exception sae) {
                        S3StoreIT.this.e = sae;
                    }
                    assertThat(e).isInstanceOf(StoreAccessException.class);
                }
            }

            @Nested
            @DisplayName("When an invalid property path is used to unset content")
            class InvalidPropertyPathIsUsedToUnsetContent {
                @BeforeEach
                void setUp() throws InterruptedException {
                    ensureTestBucket();

                    resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                    entity = new DualContentJpaLongIdMimeEntity();
                    entity.setContentType("text/plain");
                    entity.setRenditionType("text/html");
                    entity = repo.save(entity);

                    store.setContent(entity, TestSupportData.sampleTextStream());
                    store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.sampleHtmlRenditionStream());
                }

                @Test
                void shouldThrowAnError() {
                    try {
                        store.unsetContent(entity, PropertyPath.from("does.not.exist"));
                    } catch (Exception sae) {
                        S3StoreIT.this.e = sae;
                    }
                    assertThat(e).isInstanceOf(StoreAccessException.class);
                }
            }

            @Nested
            @DisplayName("When content is deleted and the content ID field is shared with entity id")
            class ContentIsDeletedAndContentIdFieldIsSharedWithEntityId {
                @BeforeEach
                void setUp() throws InterruptedException {
                    ensureTestBucket();

                    resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                    entity = new DualContentJpaLongIdMimeEntity();
                    entity.setContentType("text/plain");
                    entity.setRenditionType("text/html");
                    entity = repo.save(entity);

                    store.setContent(entity, TestSupportData.sampleTextStream());
                    store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.sampleHtmlRenditionStream());
                }

                @Test
                void shouldNotResetTheIdField() {
                    SharedIdJakartaJpaEntity sharedIdContentIdEntity = sharedIdRepository.save(new SharedIdJakartaJpaEntity());

                    sharedIdContentIdEntity = sharedIdStore.setContent(sharedIdContentIdEntity, TestSupportData.sampleTextStream());
                    sharedIdContentIdEntity = sharedIdRepository.save(sharedIdContentIdEntity);
                    String id = sharedIdContentIdEntity.getContentId();
                    sharedIdContentIdEntity = sharedIdStore.unsetContent(sharedIdContentIdEntity);
                    assertThat(sharedIdContentIdEntity.getContentId()).isEqualTo(id);
                    assertThat(sharedIdContentIdEntity.getContentLen()).isNull();
                }
            }

            @Nested
            class EmbeddedContent {
                @Nested
                @DisplayName("Given a entity with a null embedded content object")
                class EntityWithNullEmbeddedContentObject {
                    @BeforeEach
                    void setUp() throws InterruptedException {
                        ensureTestBucket();

                        resourceLocation = TestSupportData.UNASSOCIATED_RESOURCE_LOCATION;

                        entity = new DualContentJpaLongIdMimeEntity();
                        entity.setContentType("text/plain");
                        entity.setRenditionType("text/html");
                        entity = repo.save(entity);

                        store.setContent(entity, TestSupportData.sampleTextStream());
                        store.setContent(entity, PropertyPath.from("rendition"), TestSupportData.sampleHtmlRenditionStream());
                    }

                    @Test
                    void shouldReturnNullWhenContentIsFetched() throws IOException {
                        EmbeddedContentJpaStringIdEntity embedded = embeddedRepo.save(new EmbeddedContentJpaStringIdEntity());
                        assertThat(embeddedStore.getContent(embedded, PropertyPath.from("content"))).isNull();
                    }

                    @Test
                    void shouldBeSuccessfulWhenContentIsSet() throws IOException {
                        EmbeddedContentJpaStringIdEntity embedded = embeddedRepo.save(new EmbeddedContentJpaStringIdEntity());
                        embeddedStore.setContent(embedded, PropertyPath.from("content"), TestSupportData.sampleTextStream());
                        try (InputStream is = embeddedStore.getContent(embedded, PropertyPath.from("content"))) {
                            assertThat(IOUtils.contentEquals(is, TestSupportData.sampleTextStream())).isTrue();
                        }
                    }

                    @Test
                    void shouldReturnNullWhenContentIsUnset() {
                        EmbeddedContentJpaStringIdEntity embedded = embeddedRepo.save(new EmbeddedContentJpaStringIdEntity());
                        assertThat(embeddedStore.unsetContent(embedded, PropertyPath.from("content"))).isSameAs(embedded);
                    }
                }
            }
        }
    }
}
