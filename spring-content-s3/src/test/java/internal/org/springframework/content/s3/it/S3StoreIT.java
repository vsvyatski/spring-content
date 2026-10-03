package internal.org.springframework.content.s3.it;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import jakarta.persistence.*;
import java.util.Arrays;
import net.bytebuddy.utility.RandomString;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.io.RangeableResource;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.SetContentParams;
import org.springframework.content.commons.store.UnsetContentParams;
import org.springframework.content.s3.config.EnableS3Stores;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.UUID;


@SpringBootTest()
@ExtendWith(SpringExtension.class)
public class S3StoreIT {

    private static final String BUCKET = "test-bucket";

    static {
        System.setProperty("spring.content.s3.bucket", BUCKET);
    }

    private static Object mutex = new Object();

    @Autowired
    private TestEntityRepository repo;

    @Autowired
    private TestEntityStore store;

    @Autowired
    private S3Client client;

    private String resourceLocation;

    private Resource genericResource;

    private TestEntity entity;

    private Exception e;

    // Shared id tests
    @Autowired
    private SharedIdRepository sharedIdRepository;
    @Autowired
    private SharedIdStore sharedIdStore;

    // Embedded content tests
    @Autowired
    private EmbeddedRepository embeddedRepo;
    @Autowired
    private EmbeddedStore embeddedStore;

    static {
        System.setProperty("spring.content.s3.bucket", "test-bucket");
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
                        synchronized(mutex) {
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

                                                // wait for bucket to be created before continuing
                                                boolean found = false;
                                                while (!found) {
                                                    headBucketRequest = HeadBucketRequest.builder()
                                                            .bucket(BUCKET)
                                                            .build();
                                                    try {
                                                        client.headBucket(headBucketRequest);
                                                        found = true;
                                                    } catch (NoSuchBucketException e2) {
                                                    }

                                                    System.out.println("sleeping...");
                                                    Thread.sleep(100);
                                                }
                                            }
                                        }

                                        RandomString random  = new RandomString(5);
                                        resourceLocation = random.nextString();

                        genericResource = store.getResource(resourceLocation);
                    }
                    @AfterEach
                    void tearDown() throws IOException {
                        ((DeletableResource)genericResource).delete();
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
                class GivenContentIsAddedToThatResource {
                    @Nested
                    class Tests {
                        @BeforeEach
                        void setUp() throws IOException, InterruptedException {
                            synchronized(mutex) {
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

                                                    // wait for bucket to be created before continuing
                                                    boolean found = false;
                                                    while (!found) {
                                                        headBucketRequest = HeadBucketRequest.builder()
                                                                .bucket(BUCKET)
                                                                .build();
                                                        try {
                                                            client.headBucket(headBucketRequest);
                                                            found = true;
                                                        } catch (NoSuchBucketException e2) {
                                                        }

                                                        System.out.println("sleeping...");
                                                        Thread.sleep(100);
                                                    }
                                                }
                                            }

                                            RandomString random  = new RandomString(5);
                                            resourceLocation = random.nextString();

                            genericResource = store.getResource(resourceLocation);

                            try (InputStream is = new ByteArrayInputStream("Hello Spring Content World!".getBytes())) {
                                                            try (OutputStream os = ((WritableResource)genericResource).getOutputStream()) {
                                                                IOUtils.copy(is, os);
                                                            }
                                                        }
                        }
                        @AfterEach
                        void tearDown() throws IOException {
                            ((DeletableResource)genericResource).delete();
                        }
                        @Test
                        void shouldStoreThatContent() throws IOException {
                            assertThat(genericResource.exists()).isTrue();

                                                        boolean matches = false;
                                                        try (InputStream expected = new ByteArrayInputStream("Hello Spring Content World!".getBytes())) {
                                                            try (InputStream actual = genericResource.getInputStream()) {
                                                                matches = IOUtils.contentEquals(expected, actual);
                                                                assertThat(matches).isTrue();
                                                            }
                                                        }
                        }
                    }
                    @Nested
                    class GivenThatResourceIsThenUpdated {
                        @BeforeEach
                        void setUp() throws IOException, InterruptedException {
                            synchronized(mutex) {
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

                                                    // wait for bucket to be created before continuing
                                                    boolean found = false;
                                                    while (!found) {
                                                        headBucketRequest = HeadBucketRequest.builder()
                                                                .bucket(BUCKET)
                                                                .build();
                                                        try {
                                                            client.headBucket(headBucketRequest);
                                                            found = true;
                                                        } catch (NoSuchBucketException e2) {
                                                        }

                                                        System.out.println("sleeping...");
                                                        Thread.sleep(100);
                                                    }
                                                }
                                            }

                                            RandomString random  = new RandomString(5);
                                            resourceLocation = random.nextString();

                            genericResource = store.getResource(resourceLocation);

                            try (InputStream is = new ByteArrayInputStream("Hello Spring Content World!".getBytes())) {
                                                            try (OutputStream os = ((WritableResource)genericResource).getOutputStream()) {
                                                                IOUtils.copy(is, os);
                                                            }
                                                        }

                            try (InputStream is = new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes())) {
                                                                try (OutputStream os = ((WritableResource)genericResource).getOutputStream()) {
                                                                    IOUtils.copy(is, os);
                                                                }
                                                            }
                        }
                        @AfterEach
                        void tearDown() throws IOException {
                            ((DeletableResource)genericResource).delete();
                        }
                        @Test
                        void shouldStoreThatUpdatedContent() throws IOException {
                            assertThat(genericResource.exists()).isTrue();

                                                            try (InputStream expected = new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes())) {
                                                                try (InputStream actual = genericResource.getInputStream()) {
                                                                    assertThat(IOUtils.contentEquals(expected, actual)).isTrue();
                                                                }
                                                            }
                        }
                    }
                    @Nested
                    class GivenAByteRangeIsRequested {
                        @BeforeEach
                        void setUp() throws IOException, InterruptedException {
                            synchronized(mutex) {
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

                                                    // wait for bucket to be created before continuing
                                                    boolean found = false;
                                                    while (!found) {
                                                        headBucketRequest = HeadBucketRequest.builder()
                                                                .bucket(BUCKET)
                                                                .build();
                                                        try {
                                                            client.headBucket(headBucketRequest);
                                                            found = true;
                                                        } catch (NoSuchBucketException e2) {
                                                        }

                                                        System.out.println("sleeping...");
                                                        Thread.sleep(100);
                                                    }
                                                }
                                            }

                                            RandomString random  = new RandomString(5);
                                            resourceLocation = random.nextString();

                            genericResource = store.getResource(resourceLocation);

                            try (InputStream is = new ByteArrayInputStream("Hello Spring Content World!".getBytes())) {
                                                            try (OutputStream os = ((WritableResource)genericResource).getOutputStream()) {
                                                                IOUtils.copy(is, os);
                                                            }
                                                        }
                        }
                        @AfterEach
                        void tearDown() throws IOException {
                            ((DeletableResource)genericResource).delete();
                        }
                        @Test
                        void shouldReturnAPartialContentInputStreamAndThePartialContent() throws IOException {
                            ((RangeableResource)genericResource).setRange("bytes=6-19");

                                                            var expectedBytes = "Hello Spring Content World!".getBytes();
                                                            Arrays.fill(expectedBytes, 0, 6, (byte) 0); // First 5 bytes are absent
                                                            Arrays.fill(expectedBytes, 20, expectedBytes.length, (byte) 0); // Bytes after position 19 are absent

                                                            try(InputStream actual = genericResource.getInputStream()) {
                                                                var actualBytes = actual.readAllBytes();
                                                                assertThat(actualBytes).isEqualTo(expectedBytes);
                                                            }
                        }
                    }
                    @Nested
                    class GivenThatResourceIsThenDeleted {
                        @BeforeEach
                        void setUp() throws IOException, InterruptedException {
                            synchronized(mutex) {
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

                                                    // wait for bucket to be created before continuing
                                                    boolean found = false;
                                                    while (!found) {
                                                        headBucketRequest = HeadBucketRequest.builder()
                                                                .bucket(BUCKET)
                                                                .build();
                                                        try {
                                                            client.headBucket(headBucketRequest);
                                                            found = true;
                                                        } catch (NoSuchBucketException e2) {
                                                        }

                                                        System.out.println("sleeping...");
                                                        Thread.sleep(100);
                                                    }
                                                }
                                            }

                                            RandomString random  = new RandomString(5);
                                            resourceLocation = random.nextString();

                            genericResource = store.getResource(resourceLocation);

                            try (InputStream is = new ByteArrayInputStream("Hello Spring Content World!".getBytes())) {
                                                            try (OutputStream os = ((WritableResource)genericResource).getOutputStream()) {
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
                            ((DeletableResource)genericResource).delete();
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
            class GivenANewEntity {
                @Nested
                class Tests {
                    @BeforeEach
                    void setUp() throws InterruptedException {
                        synchronized(mutex) {
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

                                                // wait for bucket to be created before continuing
                                                boolean found = false;
                                                while (!found) {
                                                    headBucketRequest = HeadBucketRequest.builder()
                                                            .bucket(BUCKET)
                                                            .build();
                                                    try {
                                                        client.headBucket(headBucketRequest);
                                                        found = true;
                                                    } catch (NoSuchBucketException e2) {
                                                    }

                                                    System.out.println("sleeping...");
                                                    Thread.sleep(100);
                                                }
                                            }
                                        }

                                        RandomString random  = new RandomString(5);
                                        resourceLocation = random.nextString();

                        entity = new TestEntity();
                                                entity = repo.save(entity);
                    }
                    @Test
                    void shouldNotHaveAnAssociatedResource() {
                        assertThat(entity.getContentId()).isNull();
                                                assertThat(store.getResource(entity)).isNull();
                    }
                }
                @Nested
                class GivenAResource {
                    @Nested
                    class WhenTheResourceIsAssociated {
                        @Nested
                        class Tests {
                            @BeforeEach
                            void setUp() throws InterruptedException {
                                synchronized(mutex) {
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

                                                        // wait for bucket to be created before continuing
                                                        boolean found = false;
                                                        while (!found) {
                                                            headBucketRequest = HeadBucketRequest.builder()
                                                                    .bucket(BUCKET)
                                                                    .build();
                                                            try {
                                                                client.headBucket(headBucketRequest);
                                                                found = true;
                                                            } catch (NoSuchBucketException e2) {
                                                            }

                                                            System.out.println("sleeping...");
                                                            Thread.sleep(100);
                                                        }
                                                    }
                                                }

                                                RandomString random  = new RandomString(5);
                                                resourceLocation = random.nextString();

                                entity = new TestEntity();
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
                        class WhenTheResourceIsUnassociated {
                            @BeforeEach
                            void setUp() throws InterruptedException {
                                synchronized(mutex) {
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

                                                        // wait for bucket to be created before continuing
                                                        boolean found = false;
                                                        while (!found) {
                                                            headBucketRequest = HeadBucketRequest.builder()
                                                                    .bucket(BUCKET)
                                                                    .build();
                                                            try {
                                                                client.headBucket(headBucketRequest);
                                                                found = true;
                                                            } catch (NoSuchBucketException e2) {
                                                            }

                                                            System.out.println("sleeping...");
                                                            Thread.sleep(100);
                                                        }
                                                    }
                                                }

                                                RandomString random  = new RandomString(5);
                                                resourceLocation = random.nextString();

                                entity = new TestEntity();
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
                        class WhenAInvalidPropertyPathIsUsedToAssociateAResource {
                            @BeforeEach
                            void setUp() throws InterruptedException {
                                synchronized(mutex) {
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

                                                        // wait for bucket to be created before continuing
                                                        boolean found = false;
                                                        while (!found) {
                                                            headBucketRequest = HeadBucketRequest.builder()
                                                                    .bucket(BUCKET)
                                                                    .build();
                                                            try {
                                                                client.headBucket(headBucketRequest);
                                                                found = true;
                                                            } catch (NoSuchBucketException e2) {
                                                            }

                                                            System.out.println("sleeping...");
                                                            Thread.sleep(100);
                                                        }
                                                    }
                                                }

                                                RandomString random  = new RandomString(5);
                                                resourceLocation = random.nextString();

                                entity = new TestEntity();
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
                        class WhenAInvalidPropertyPathIsUsedToLoadAResource {
                            @BeforeEach
                            void setUp() throws InterruptedException {
                                synchronized(mutex) {
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

                                                        // wait for bucket to be created before continuing
                                                        boolean found = false;
                                                        while (!found) {
                                                            headBucketRequest = HeadBucketRequest.builder()
                                                                    .bucket(BUCKET)
                                                                    .build();
                                                            try {
                                                                client.headBucket(headBucketRequest);
                                                                found = true;
                                                            } catch (NoSuchBucketException e2) {
                                                            }

                                                            System.out.println("sleeping...");
                                                            Thread.sleep(100);
                                                        }
                                                    }
                                                }

                                                RandomString random  = new RandomString(5);
                                                resourceLocation = random.nextString();

                                entity = new TestEntity();
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
                        class WhenAInvalidPropertyPathIsUsedToUnassociateAResource {
                            @BeforeEach
                            void setUp() throws InterruptedException {
                                synchronized(mutex) {
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

                                                        // wait for bucket to be created before continuing
                                                        boolean found = false;
                                                        while (!found) {
                                                            headBucketRequest = HeadBucketRequest.builder()
                                                                    .bucket(BUCKET)
                                                                    .build();
                                                            try {
                                                                client.headBucket(headBucketRequest);
                                                                found = true;
                                                            } catch (NoSuchBucketException e2) {
                                                            }

                                                            System.out.println("sleeping...");
                                                            Thread.sleep(100);
                                                        }
                                                    }
                                                }

                                                RandomString random  = new RandomString(5);
                                                resourceLocation = random.nextString();

                                entity = new TestEntity();
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
                    synchronized(mutex) {
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

                                            // wait for bucket to be created before continuing
                                            boolean found = false;
                                            while (!found) {
                                                headBucketRequest = HeadBucketRequest.builder()
                                                        .bucket(BUCKET)
                                                        .build();
                                                try {
                                                    client.headBucket(headBucketRequest);
                                                    found = true;
                                                } catch (NoSuchBucketException e2) {
                                                }

                                                System.out.println("sleeping...");
                                                Thread.sleep(100);
                                            }
                                        }
                                    }

                                    RandomString random  = new RandomString(5);
                                    resourceLocation = random.nextString();

                    entity = new TestEntity();
                    //                    entity.setContentType("text/plain");
                    //                    entity.setContentType("text/html");
                                        entity = repo.save(entity);

                                        store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                        store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));
                }
                @Test
                void shouldBeAbleToStoreNewContent() {
                    // content
                                        try (InputStream content = store.getContent(entity)) {
                                            assertThat(IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring Content World!".getBytes()), content)).isTrue();
                                        } catch (IOException ioe) {}

                                        //rendition
                                        try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                                            assertThat(IOUtils.contentEquals(new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()), content)).isTrue();
                                        } catch (IOException ioe) {}
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
                }
            }
            @Nested
            class WhenContentIsUpdated {
                @BeforeEach
                void setUp() throws InterruptedException {
                    synchronized(mutex) {
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

                                            // wait for bucket to be created before continuing
                                            boolean found = false;
                                            while (!found) {
                                                headBucketRequest = HeadBucketRequest.builder()
                                                        .bucket(BUCKET)
                                                        .build();
                                                try {
                                                    client.headBucket(headBucketRequest);
                                                    found = true;
                                                } catch (NoSuchBucketException e2) {
                                                }

                                                System.out.println("sleeping...");
                                                Thread.sleep(100);
                                            }
                                        }
                                    }

                                    RandomString random  = new RandomString(5);
                                    resourceLocation = random.nextString();

                    entity = new TestEntity();
                    //                    entity.setContentType("text/plain");
                    //                    entity.setContentType("text/html");
                                        entity = repo.save(entity);

                                        store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                        store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));

                    store.setContent(entity, new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()));
                                            store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Updated Spring Content World!</html>".getBytes()));
                                            entity = repo.save(entity);
                }
                @Test
                void shouldHaveTheUpdatedContent() throws IOException {
                    //content
                                            boolean matches = false;
                                            try (InputStream content = store.getContent(entity)) {
                                                matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), content);
                                                assertThat(matches).isTrue();
                                            }

                                            //rendition
                                            matches = false;
                                            try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                                                matches = IOUtils.contentEquals(new ByteArrayInputStream("<html>Hello Updated Spring Content World!</html>".getBytes()), content);
                                                assertThat(matches).isTrue();
                                            }
                }
            }
            @Nested
            class WhenContentIsUpdatedWithShorterContent {
                @BeforeEach
                void setUp() throws InterruptedException {
                    synchronized(mutex) {
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

                                            // wait for bucket to be created before continuing
                                            boolean found = false;
                                            while (!found) {
                                                headBucketRequest = HeadBucketRequest.builder()
                                                        .bucket(BUCKET)
                                                        .build();
                                                try {
                                                    client.headBucket(headBucketRequest);
                                                    found = true;
                                                } catch (NoSuchBucketException e2) {
                                                }

                                                System.out.println("sleeping...");
                                                Thread.sleep(100);
                                            }
                                        }
                                    }

                                    RandomString random  = new RandomString(5);
                                    resourceLocation = random.nextString();

                    entity = new TestEntity();
                    //                    entity.setContentType("text/plain");
                    //                    entity.setContentType("text/html");
                                        entity = repo.save(entity);

                                        store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                        store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));

                    store.setContent(entity, new ByteArrayInputStream("Hello Spring World!".getBytes()));
                                            store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring World!</html>".getBytes()));
                                            entity = repo.save(entity);
                }
                @Test
                void shouldStoreOnlyTheNewContent() throws IOException {
                    //content
                                            boolean matches = false;
                                            try (InputStream content = store.getContent(entity)) {
                                                matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring World!".getBytes()), content);
                                                assertThat(matches).isTrue();
                                            }

                                            //rendition
                                            matches = false;
                                            try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                                                matches = IOUtils.contentEquals(new ByteArrayInputStream("<html>Hello Spring World!</html>".getBytes()), content);
                                                assertThat(matches).isTrue();
                                            }
                }
            }
            @Nested
            class WhenContentIsUpdatedAndNotOverwritten {
                @BeforeEach
                void setUp() throws InterruptedException {
                    synchronized(mutex) {
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

                                            // wait for bucket to be created before continuing
                                            boolean found = false;
                                            while (!found) {
                                                headBucketRequest = HeadBucketRequest.builder()
                                                        .bucket(BUCKET)
                                                        .build();
                                                try {
                                                    client.headBucket(headBucketRequest);
                                                    found = true;
                                                } catch (NoSuchBucketException e2) {
                                                }

                                                System.out.println("sleeping...");
                                                Thread.sleep(100);
                                            }
                                        }
                                    }

                                    RandomString random  = new RandomString(5);
                                    resourceLocation = random.nextString();

                    entity = new TestEntity();
                    //                    entity.setContentType("text/plain");
                    //                    entity.setContentType("text/html");
                                        entity = repo.save(entity);

                                        store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                        store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));
                }
                @Test
                void shouldHaveTheUpdatedContent() throws IOException {
                    String contentId = entity.getContentId();
                                            client.headObject(HeadObjectRequest.builder().bucket(BUCKET).key(contentId).build());

                                            store.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), new SetContentParams(-1, true, SetContentParams.ContentDisposition.CreateNew));
                                            entity = repo.save(entity);

                                            boolean matches = false;
                                            try (InputStream content = store.getContent(entity)) {
                                                matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), content);
                                                assertThat(matches).isTrue();
                                            }

                                            assertThat(entity.getContentId()).isNotEqualTo(contentId);
                                            client.headObject(HeadObjectRequest.builder().bucket(BUCKET).key(entity.getContentId()).build());
                }
            }
            @Nested
            class WhenContentIsUnset {
                @BeforeEach
                void setUp() throws InterruptedException {
                    synchronized(mutex) {
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

                                            // wait for bucket to be created before continuing
                                            boolean found = false;
                                            while (!found) {
                                                headBucketRequest = HeadBucketRequest.builder()
                                                        .bucket(BUCKET)
                                                        .build();
                                                try {
                                                    client.headBucket(headBucketRequest);
                                                    found = true;
                                                } catch (NoSuchBucketException e2) {
                                                }

                                                System.out.println("sleeping...");
                                                Thread.sleep(100);
                                            }
                                        }
                                    }

                                    RandomString random  = new RandomString(5);
                                    resourceLocation = random.nextString();

                    entity = new TestEntity();
                    //                    entity.setContentType("text/plain");
                    //                    entity.setContentType("text/html");
                                        entity = repo.save(entity);

                                        store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                        store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));

                    resourceLocation = entity.getContentId().toString();
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

                                            try {
                                                client.headObject(HeadObjectRequest.builder().bucket(BUCKET).key(resourceLocation).build());
                                                fail("expected content to be removed but is still exists");
                                            } catch (NoSuchKeyException nske) {
                                            }

                                            //rendition
                                            try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                                                assertThat(content).isNull();
                                            }

                                            assertThat(entity.getRenditionId()).isNull();
                                            assertThat(entity.getRenditionLen()).isEqualTo(0L);
                }
            }
            @Nested
            class WhenContentIsUnsetButKept {
                @BeforeEach
                void setUp() throws InterruptedException {
                    synchronized(mutex) {
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

                                            // wait for bucket to be created before continuing
                                            boolean found = false;
                                            while (!found) {
                                                headBucketRequest = HeadBucketRequest.builder()
                                                        .bucket(BUCKET)
                                                        .build();
                                                try {
                                                    client.headBucket(headBucketRequest);
                                                    found = true;
                                                } catch (NoSuchBucketException e2) {
                                                }

                                                System.out.println("sleeping...");
                                                Thread.sleep(100);
                                            }
                                        }
                                    }

                                    RandomString random  = new RandomString(5);
                                    resourceLocation = random.nextString();

                    entity = new TestEntity();
                    //                    entity.setContentType("text/plain");
                    //                    entity.setContentType("text/html");
                                        entity = repo.save(entity);

                                        store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                        store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));

                    resourceLocation = entity.getContentId().toString();
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
            class WhenAnInvalidPropertyPathIsUsedToSetContent {
                @BeforeEach
                void setUp() throws InterruptedException {
                    synchronized(mutex) {
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

                                            // wait for bucket to be created before continuing
                                            boolean found = false;
                                            while (!found) {
                                                headBucketRequest = HeadBucketRequest.builder()
                                                        .bucket(BUCKET)
                                                        .build();
                                                try {
                                                    client.headBucket(headBucketRequest);
                                                    found = true;
                                                } catch (NoSuchBucketException e2) {
                                                }

                                                System.out.println("sleeping...");
                                                Thread.sleep(100);
                                            }
                                        }
                                    }

                                    RandomString random  = new RandomString(5);
                                    resourceLocation = random.nextString();

                    entity = new TestEntity();
                    //                    entity.setContentType("text/plain");
                    //                    entity.setContentType("text/html");
                                        entity = repo.save(entity);

                                        store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                        store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));
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
            class WhenAnInvalidPropertyPathIsUsedToGetContent {
                @BeforeEach
                void setUp() throws InterruptedException {
                    synchronized(mutex) {
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

                                            // wait for bucket to be created before continuing
                                            boolean found = false;
                                            while (!found) {
                                                headBucketRequest = HeadBucketRequest.builder()
                                                        .bucket(BUCKET)
                                                        .build();
                                                try {
                                                    client.headBucket(headBucketRequest);
                                                    found = true;
                                                } catch (NoSuchBucketException e2) {
                                                }

                                                System.out.println("sleeping...");
                                                Thread.sleep(100);
                                            }
                                        }
                                    }

                                    RandomString random  = new RandomString(5);
                                    resourceLocation = random.nextString();

                    entity = new TestEntity();
                    //                    entity.setContentType("text/plain");
                    //                    entity.setContentType("text/html");
                                        entity = repo.save(entity);

                                        store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                        store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));
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
            class WhenAnInvalidPropertyPathIsUsedToUnsetContent {
                @BeforeEach
                void setUp() throws InterruptedException {
                    synchronized(mutex) {
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

                                            // wait for bucket to be created before continuing
                                            boolean found = false;
                                            while (!found) {
                                                headBucketRequest = HeadBucketRequest.builder()
                                                        .bucket(BUCKET)
                                                        .build();
                                                try {
                                                    client.headBucket(headBucketRequest);
                                                    found = true;
                                                } catch (NoSuchBucketException e2) {
                                                }

                                                System.out.println("sleeping...");
                                                Thread.sleep(100);
                                            }
                                        }
                                    }

                                    RandomString random  = new RandomString(5);
                                    resourceLocation = random.nextString();

                    entity = new TestEntity();
                    //                    entity.setContentType("text/plain");
                    //                    entity.setContentType("text/html");
                                        entity = repo.save(entity);

                                        store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                        store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));
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
            class WhenContentIsDeletedAndTheContentIdFieldIsSharedWithEntityId {
                @BeforeEach
                void setUp() throws InterruptedException {
                    synchronized(mutex) {
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

                                            // wait for bucket to be created before continuing
                                            boolean found = false;
                                            while (!found) {
                                                headBucketRequest = HeadBucketRequest.builder()
                                                        .bucket(BUCKET)
                                                        .build();
                                                try {
                                                    client.headBucket(headBucketRequest);
                                                    found = true;
                                                } catch (NoSuchBucketException e2) {
                                                }

                                                System.out.println("sleeping...");
                                                Thread.sleep(100);
                                            }
                                        }
                                    }

                                    RandomString random  = new RandomString(5);
                                    resourceLocation = random.nextString();

                    entity = new TestEntity();
                    //                    entity.setContentType("text/plain");
                    //                    entity.setContentType("text/html");
                                        entity = repo.save(entity);

                                        store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                        store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));
                }
                @Test
                void shouldNotResetTheIdField() {
                    SharedIdContentIdEntity sharedIdContentIdEntity = sharedIdRepository.save(new SharedIdContentIdEntity());

                                            sharedIdContentIdEntity = sharedIdStore.setContent(sharedIdContentIdEntity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                            sharedIdContentIdEntity = sharedIdRepository.save(sharedIdContentIdEntity);
                                            String id = sharedIdContentIdEntity.getContentId();
                                            sharedIdContentIdEntity = sharedIdStore.unsetContent(sharedIdContentIdEntity);
                                            assertThat(sharedIdContentIdEntity.getContentId()).isEqualTo(id);
                                            assertThat(sharedIdContentIdEntity.getContentLen()).isEqualTo(0L);
                }
            }
            @Nested
            class EmbeddedContent {
                @Nested
                class GivenAEntityWithANullEmbeddedContentObject {
                    @BeforeEach
                    void setUp() throws InterruptedException {
                        synchronized(mutex) {
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

                                                // wait for bucket to be created before continuing
                                                boolean found = false;
                                                while (!found) {
                                                    headBucketRequest = HeadBucketRequest.builder()
                                                            .bucket(BUCKET)
                                                            .build();
                                                    try {
                                                        client.headBucket(headBucketRequest);
                                                        found = true;
                                                    } catch (NoSuchBucketException e2) {
                                                    }

                                                    System.out.println("sleeping...");
                                                    Thread.sleep(100);
                                                }
                                            }
                                        }

                                        RandomString random  = new RandomString(5);
                                        resourceLocation = random.nextString();

                        entity = new TestEntity();
                        //                    entity.setContentType("text/plain");
                        //                    entity.setContentType("text/html");
                                            entity = repo.save(entity);

                                            store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                            store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));
                    }
                    @Test
                    void shouldReturnNullWhenContentIsFetched() throws IOException {
                        EntityWithEmbeddedContent entity = embeddedRepo.save(new EntityWithEmbeddedContent());
                                                    assertThat(embeddedStore.getContent(entity, PropertyPath.from("content"))).isNull();
                    }
                    @Test
                    void shouldBeSuccessfulWhenContentIsSet() throws IOException {
                        EntityWithEmbeddedContent entity = embeddedRepo.save(new EntityWithEmbeddedContent());
                                                    embeddedStore.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                                    try (InputStream is = embeddedStore.getContent(entity, PropertyPath.from("content"))) {
                                                        assertThat(IOUtils.contentEquals(is, new ByteArrayInputStream("Hello Spring Content World!".getBytes()))).isTrue();
                                                    }
                    }
                    @Test
                    void shouldReturnNullWhenContentIsUnset() {
                        EntityWithEmbeddedContent entity = embeddedRepo.save(new EntityWithEmbeddedContent());
                                                    assertThat(embeddedStore.unsetContent(entity, PropertyPath.from("content"))).isSameAs(entity);
                    }
                }
            }
        }
    }


    @Test
    public void noop() {}

    @SpringBootApplication()
    @EnableJpaRepositories(considerNestedRepositories = true)
    @EnableS3Stores
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

    @Entity
    public static class TestEntity {

        @Id
        @GeneratedValue(strategy=GenerationType.AUTO)
        private Long id;

        @ContentId
        private String contentId;

        @ContentLength
        private Long contentLen;

        @MimeType
        private String contentType;

        @ContentId
        private String renditionId;

        @ContentLength
        private long renditionLen;

        @MimeType
        private String renditionType;

        public TestEntity() {
        }

        public TestEntity(String contentId) {
            this.contentId = new String(contentId);
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getContentId() {
            return contentId;
        }

        public void setContentId(String contentId) {
            this.contentId = contentId;
        }

        public Long getContentLen() {
            return contentLen;
        }

        public void setContentLen(Long contentLen) {
            this.contentLen = contentLen;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public String getRenditionId() {
            return renditionId;
        }

        public void setRenditionId(String renditionId) {
            this.renditionId = renditionId;
        }

        public long getRenditionLen() {
            return renditionLen;
        }

        public void setRenditionLen(long renditionLen) {
            this.renditionLen = renditionLen;
        }

        public String getRenditionType() {
            return renditionType;
        }

        public void setRenditionType(String renditionType) {
            this.renditionType = renditionType;
        }
    }

    public interface TestEntityRepository extends JpaRepository<TestEntity, Long> {}
    public interface TestEntityStore extends ContentStore<TestEntity, String> {}

    @Entity
    public static class SharedIdContentIdEntity {

        @jakarta.persistence.Id
        @ContentId
        private String contentId = UUID.randomUUID().toString();

        @ContentLength
        private long contentLen;

        public SharedIdContentIdEntity() {
        }

        public String getContentId() {
            return contentId;
        }

        public void setContentId(String contentId) {
            this.contentId = contentId;
        }

        public long getContentLen() {
            return contentLen;
        }

        public void setContentLen(long contentLen) {
            this.contentLen = contentLen;
        }
    }

    public interface SharedIdRepository extends JpaRepository<SharedIdContentIdEntity, String> {}
    public interface SharedIdStore extends ContentStore<SharedIdContentIdEntity, String> {}

    @Entity
    @Table(name="entity_with_embedded")
    public static class EntityWithEmbeddedContent {

        @Id
        private String id = UUID.randomUUID().toString();

        @Embedded
        private EmbeddedContent content;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public EmbeddedContent getContent() {
            return content;
        }

        public void setContent(EmbeddedContent content) {
            this.content = content;
        }
    }

    @Embeddable
    public static class EmbeddedContent {

        @ContentId
        private String contentId;

        @ContentLength
        private Long contentLen;

        public String getContentId() {
            return contentId;
        }

        public void setContentId(String contentId) {
            this.contentId = contentId;
        }

        public Long getContentLen() {
            return contentLen;
        }

        public void setContentLen(Long contentLen) {
            this.contentLen = contentLen;
        }
    }

    public interface EmbeddedRepository extends JpaRepository<EntityWithEmbeddedContent, String> {}
    public interface EmbeddedStore extends ContentStore<EntityWithEmbeddedContent, String> {}
}
