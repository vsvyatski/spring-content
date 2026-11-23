package internal.org.springframework.content.s3.store;

import internal.org.springframework.content.s3.config.S3StoreConfiguration;
import internal.org.springframework.content.s3.io.S3StoreResource;
import internal.org.springframework.content.s3.io.SimpleStorageProtocolResolver;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.config.ContentPropertyInfo;
import org.springframework.content.commons.io.RangeableResource;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.content.commons.utils.PlacementServiceImpl;
import org.springframework.content.s3.Bucket;
import org.springframework.content.s3.S3ObjectId;
import org.springframework.content.s3.config.MultiTenantS3ClientProvider;
import org.springframework.content.test.support.ContentAccessor;
import org.springframework.content.test.support.SingleContentFields;
import org.springframework.content.test.support.jpa.SharedIdJakartaJpaEntity;
import org.springframework.content.test.support.jpa.SharedIdSpringJpaEntity;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.*;
import java.util.UUID;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.content.test.support.TestSupportData.SAMPLE_CONTENT_ID;

public class DefaultS3StoreImplTest {

    private DefaultS3StoreImpl<ContentAccessor<String>, String> s3StoreImpl;
    private DefaultS3StoreImpl<ContentAccessor<String>, S3ObjectId> s3ObjectIdBasedStore;
    private DefaultS3StoreImpl<ContentAccessor<String>, CustomContentId> customS3ContentIdBasedStore;

    private GenericApplicationContext context = new GenericApplicationContext();
    private ResourceLoader loader;
    private PlacementService placementService;
    private S3Client client, client2;

    private String defaultBucket;

    private CustomContentId customId;
    private ContentAccessor<String> entity;

    private WritableResource resource;
    private Resource r, nonExistentResource;
    private InputStream content;
    private OutputStream output;
    private InputStream result;
    private Exception e;

    @SuppressWarnings("DataFlowIssue")
    private static S3ObjectId objectIdWithNullBucket(String key) {
        return new S3ObjectId(null, key);
    }

    public static class TestEntityWithBucketAnnotation extends SingleContentFields {
        @Bucket
        private final String bucketId;

        public TestEntityWithBucketAnnotation(String bucketId) {
            this.bucketId = bucketId;
        }

        @SuppressWarnings("unused") // read by {@link org.springframework.content.commons.utils.BeanUtils} for @Bucket
        public String getBucketId() {
            return bucketId;
        }
    }

    public record CustomContentId(String customer, String objectId) implements Serializable {

        @Serial
        private static final long serialVersionUID = 7340883227287556823L;
    }

    private static final class MatchingSingleContentFieldsStringContentPropertyInfoConverter
        implements Converter<ContentPropertyInfo<SingleContentFields, String>, S3ObjectId> {
        @Override
        public S3ObjectId convert(@NonNull ContentPropertyInfo<SingleContentFields, String> source) {
            return new S3ObjectId("custom-bucket", "test-entity/custom-object-id-string-based");
        }
    }

    private static final class NonMatchingUuidContentPropertyInfoConverter
        implements Converter<ContentPropertyInfo<Object, UUID>, S3ObjectId> {
        @Override
        public S3ObjectId convert(@NonNull ContentPropertyInfo<Object, UUID> source) {
            return new S3ObjectId("custom-bucket", "object/custom-object-id-uuid-based");
        }
    }

    private static final class NonMatchingBucketEntityContentPropertyInfoConverter
        implements Converter<ContentPropertyInfo<TestEntityWithBucketAnnotation, String>, S3ObjectId> {
        @Override
        public S3ObjectId convert(@NonNull ContentPropertyInfo<TestEntityWithBucketAnnotation, String> source) {
            return new S3ObjectId("custom-bucket", "test-entity-with-bucket/custom-object-id-string-based");
        }
    }

    private static final class NullBucketContentPropertyInfoConverter
        implements Converter<ContentPropertyInfo<SingleContentFields, String>, S3ObjectId> {
        @Override
        public S3ObjectId convert(@NonNull ContentPropertyInfo<SingleContentFields, String> source) {
            return objectIdWithNullBucket("custom-object-id");
        }
    }

    @Nested
    class DefaultS3StoreImplCases {
        @Nested
        class Store {
            @Nested
            class GetResource {
                @Nested
                @DisplayName("Given the store sid is an s3object id type")
                class StoreSIDIsS3ObjectIdType {
                    @BeforeEach
                    void setUp() {
                        resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                        loader = mock(ResourceLoader.class);
                        placementService = mock(PlacementService.class);
                        client = mock(S3Client.class);
                        defaultBucket = null;

                        context.registerBean("s3Client", S3Client.class, () -> client);
                        context = new GenericApplicationContext();
                        context.refresh();

                        placementService = new PlacementServiceImpl();
                        S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
                        placementService.addConverter(String.class, String.class, source -> "/some/object/id");

                        SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver(client);
                        s3Protocol.afterPropertiesSet();
                        loader = new DefaultResourceLoader();
                        ((DefaultResourceLoader) loader).addProtocolResolver(s3Protocol);

                        s3ObjectIdBasedStore = new DefaultS3StoreImpl<>(context, loader, null, placementService, client, null);

                        try {
                            r = s3ObjectIdBasedStore.getResource(new S3ObjectId("some-defaultBucket", "some-object-id"));
                        } catch (Exception e) {
                            DefaultS3StoreImplTest.this.e = e;
                        }
                    }

                    @Test
                    void shouldReturnResource() {
                        assertThat(e).isNull();
                        assertThat(r).isInstanceOf(S3StoreResource.class);
                        assertThat(((S3StoreResource) r).getClient()).isEqualTo(client);
                        assertThat(r.getDescription()).isEqualTo(format("Amazon s3 resource [bucket='%s' and object='%s']", "some-defaultBucket", "some/object/id"));
                    }
                }

                @Nested
                @DisplayName("Given the store sid is a custom id type")
                class StoreSIDIsCustomIDType {
                    @Nested
                    @DisplayName("Given a default bucket is set")
                    class DefaultBucketIsSet {
                        @Nested
                        @DisplayName("Given the resolver is created with the static constructor function")
                        class ResolverIsCreatedWithStaticConstructorFunction {
                            @Nested
                            @DisplayName("Given an id")
                            class ID {
                                @BeforeEach
                                void setUp() {
                                    resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                    loader = mock(ResourceLoader.class);
                                    placementService = mock(PlacementService.class);
                                    client = mock(S3Client.class);
                                    defaultBucket = null;

                                    context.registerBean("s3Client", S3Client.class, () -> client);
                                    context = new GenericApplicationContext();
                                    context.refresh();

                                    defaultBucket = "default-customer";

                                    placementService = new PlacementServiceImpl();
                                    S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
                                    placementService.addConverter(CustomContentId.class, S3ObjectId.class,
                                        entity -> new S3ObjectId(entity.customer(), entity.objectId()));

                                    SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver(client);
                                    s3Protocol.afterPropertiesSet();
                                    loader = new DefaultResourceLoader();
                                    ((DefaultResourceLoader) loader).addProtocolResolver(s3Protocol);

                                    customId = new CustomContentId(
                                        "some-customer",
                                        "some-object-id");

                                    customS3ContentIdBasedStore = new DefaultS3StoreImpl<>(context, loader, null, placementService, client, null);

                                    try {
                                        r = customS3ContentIdBasedStore.getResource(customId);
                                    } catch (Exception e) {
                                        DefaultS3StoreImplTest.this.e = e;
                                    }
                                }

                                @Test
                                void shouldFetchResource() {
                                    assertThat(e).isNull();
                                    assertThat(r).isInstanceOf(S3StoreResource.class);
                                    assertThat(((S3StoreResource) r).getClient()).isEqualTo(client);
                                    assertThat(r.getDescription()).isEqualTo(format("Amazon s3 resource [bucket='%s' and object='%s']", "some-customer", "some-object-id"));
                                }
                            }
                        }
                    }

                    @Nested
                    @DisplayName("Given a default bucket is not set")
                    class DefaultBucketIsNotSet {
                        @Nested
                        @DisplayName("Given a resolver that does not validate")
                        class ResolverThatDoesNotValidate {
                            @Nested
                            @DisplayName("When called with an id that doesnt specify a bucket either")
                            class CalledWithIDThatDoesntSpecifyBucketEither {
                                @BeforeEach
                                void setUp() {
                                    resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                    loader = mock(ResourceLoader.class);
                                    placementService = mock(PlacementService.class);
                                    client = mock(S3Client.class);
                                    defaultBucket = null;

                                    context.registerBean("s3Client", S3Client.class, () -> client);
                                    context = new GenericApplicationContext();
                                    context.refresh();

                                    defaultBucket = null;

                                    placementService = new PlacementServiceImpl();
                                    S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

                                    customId = new CustomContentId(null, "some-object-id");

                                    customS3ContentIdBasedStore = new DefaultS3StoreImpl<>(context, loader, null, placementService, client, null);

                                    try {
                                        r = customS3ContentIdBasedStore.getResource(customId);
                                    } catch (Exception e) {
                                        DefaultS3StoreImplTest.this.e = e;
                                    }
                                }

                                @Test
                                void shouldThrowError() {
                                    assertThat(e).isInstanceOf(ConversionFailedException.class);
                                }
                            }
                        }
                    }
                }

                @Nested
                @DisplayName("Given a multi tenant configuration")
                class MultiTenantConfiguration {
                    @BeforeEach
                    void setUp() {
                        resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                        loader = mock(ResourceLoader.class);
                        placementService = mock(PlacementService.class);
                        client = mock(S3Client.class);
                        defaultBucket = null;

                        context.registerBean("s3Client", S3Client.class, () -> client);
                        context = new GenericApplicationContext();
                        context.refresh();

                        client2 = mock(S3Client.class);
                        MultiTenantS3ClientProvider clientProvider = () -> client2;

                        placementService = new PlacementServiceImpl();
                        S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
                        s3ObjectIdBasedStore = new DefaultS3StoreImpl<>(context, loader, null, placementService, client, clientProvider);

                        try {
                            r = s3ObjectIdBasedStore.getResource(new S3ObjectId("some-bucket", "some-object-id"));
                        } catch (Exception e) {
                            DefaultS3StoreImplTest.this.e = e;
                        }
                    }

                    @Test
                    void shouldFetchResourceUsingCorrectClient() {
                        assertThat(e).isNull();
                        assertThat(r).isInstanceOf(S3StoreResource.class);
                        assertThat(((S3StoreResource) r).getClient()).isEqualTo(client2);
                        assertThat(r.getDescription()).isEqualTo(format("Amazon s3 resource [bucket='%s' and object='%s']", "some-bucket", "some-object-id"));
                    }
                }
            }
        }

        @Nested
        class AssociativeStore {
            @Nested
            class GetResource {
                @Nested
                @DisplayName("Given the default associative store ID resolver")
                class DefaultAssociativeStoreIdResolver {
                    @Nested
                    @DisplayName("Given a default bucket")
                    class DefaultBucket {
                        @Nested
                        @DisplayName("When called with an entity that doesnt have an bucket value")
                        class CalledWithEntityThatDoesntHaveBucketValue {
                            @BeforeEach
                            void setUp() {
                                resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                loader = mock(ResourceLoader.class);
                                placementService = mock(PlacementService.class);
                                client = mock(S3Client.class);
                                defaultBucket = null;

                                context.registerBean("s3Client", S3Client.class, () -> client);
                                context = new GenericApplicationContext();
                                context.refresh();

                                defaultBucket = "default-defaultBucket";

                                entity = new SingleContentFields();
                                entity.setContentId(SAMPLE_CONTENT_ID);

                                placementService = new PlacementServiceImpl();
                                S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
                                placementService.addConverter(S3ObjectId.class, String.class,
                                    source -> "/" + source.getKey().replace("-", "/"));

                                SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver(client);
                                s3Protocol.afterPropertiesSet();
                                loader = new DefaultResourceLoader();
                                ((DefaultResourceLoader) loader).addProtocolResolver(s3Protocol);

                                s3StoreImpl = new DefaultS3StoreImpl<>(context, loader, null, placementService, client, null);

                                try {
                                    r = s3StoreImpl.getResource(entity);
                                } catch (Exception e) {
                                    DefaultS3StoreImplTest.this.e = e;
                                }
                            }

                            @Test
                            void shouldFetchResource() {
                                assertThat(e).isNull();
                                assertThat(r).isInstanceOf(S3StoreResource.class);
                                assertThat(((S3StoreResource) r).getClient()).isEqualTo(client);
                                assertThat(r.getDescription()).isEqualTo(format("Amazon s3 resource [bucket='%s' and object='%s']", "default-defaultBucket", "12345/67890"));
                            }
                        }

                        @Nested
                        @DisplayName("When called with an entity that has an bucket value")
                        class CalledWithEntityThatHasBucketValue {
                            @BeforeEach
                            void setUp() {
                                resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                loader = mock(ResourceLoader.class);
                                placementService = mock(PlacementService.class);
                                client = mock(S3Client.class);
                                defaultBucket = null;

                                context.registerBean("s3Client", S3Client.class, () -> client);
                                context = new GenericApplicationContext();
                                context.refresh();

                                defaultBucket = "default-defaultBucket";

                                entity = new TestEntityWithBucketAnnotation(
                                    "some-other-bucket");
                                entity.setContentId(SAMPLE_CONTENT_ID);

                                placementService = new PlacementServiceImpl();
                                S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

                                SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver(client);
                                s3Protocol.afterPropertiesSet();
                                loader = new DefaultResourceLoader();
                                ((DefaultResourceLoader) loader).addProtocolResolver(s3Protocol);

                                s3StoreImpl = new DefaultS3StoreImpl<>(context, loader, null, placementService, client, null);

                                try {
                                    r = s3StoreImpl.getResource(entity);
                                } catch (Exception e) {
                                    DefaultS3StoreImplTest.this.e = e;
                                }
                            }

                            @Test
                            void shouldFetchCorrectResource() {
                                assertThat(e).isNull();
                                assertThat(r).isInstanceOf(S3StoreResource.class);
                                assertThat(((S3StoreResource) r).getClient()).isEqualTo(client);
                                assertThat(r.getDescription()).isEqualTo(format("Amazon s3 resource [bucket='%s' and object='%s']", "some-other-bucket", SAMPLE_CONTENT_ID));
                            }
                        }

                        @Nested
                        @DisplayName("When called with an entity that has no associated resource")
                        class CalledWithEntityThatHasNoAssociatedResource {
                            @BeforeEach
                            void setUp() {
                                resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                loader = mock(ResourceLoader.class);
                                placementService = mock(PlacementService.class);
                                client = mock(S3Client.class);
                                defaultBucket = null;

                                context.registerBean("s3Client", S3Client.class, () -> client);
                                context = new GenericApplicationContext();
                                context.refresh();

                                defaultBucket = "default-defaultBucket";

                                entity = new SingleContentFields();

                                s3StoreImpl = new DefaultS3StoreImpl<>(context, loader, null, placementService, client, null);

                                try {
                                    r = s3StoreImpl.getResource(entity);
                                } catch (Exception e) {
                                    DefaultS3StoreImplTest.this.e = e;
                                }
                            }

                            @Test
                            void shouldReturnNull() {
                                assertThat(r).isNull();
                                assertThat(e).isNull();
                            }
                        }
                    }
                }

                @Nested
                @DisplayName("Given a custom id resolver")
                class CustomIdResolver {
                    @Nested
                    @DisplayName("Given a default bucket")
                    class DefaultBucket {
                        @Nested
                        @DisplayName("When called with an entity")
                        class CalledWithEntity {
                            @BeforeEach
                            void setUp() {
                                resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                loader = mock(ResourceLoader.class);
                                placementService = mock(PlacementService.class);
                                client = mock(S3Client.class);
                                defaultBucket = null;

                                context.registerBean("s3Client", S3Client.class, () -> client);
                                context = new GenericApplicationContext();
                                context.refresh();

                                defaultBucket = "default-defaultBucket";

                                entity = new SingleContentFields();
                                entity.setContentId(SAMPLE_CONTENT_ID);

                                placementService = new PlacementServiceImpl();
                                S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
                                placementService.addConverter(SingleContentFields.class, S3ObjectId.class,
                                    source -> new S3ObjectId("custom-bucket", "custom-object-id"));

                                SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver(client);
                                s3Protocol.afterPropertiesSet();
                                loader = new DefaultResourceLoader();
                                ((DefaultResourceLoader) loader).addProtocolResolver(s3Protocol);

                                s3StoreImpl = new DefaultS3StoreImpl<>(context, loader, null, placementService, client, null);

                                try {
                                    r = s3StoreImpl.getResource(entity);
                                } catch (Exception e) {
                                    DefaultS3StoreImplTest.this.e = e;
                                }
                            }

                            @Test
                            void shouldFetchResource() {
                                assertThat(e).isNull();
                                assertThat(r).isInstanceOf(S3StoreResource.class);
                                assertThat(((S3StoreResource) r).getClient()).isEqualTo(client);
                                assertThat(r.getDescription()).isEqualTo(format("Amazon s3 resource [bucket='%s' and object='%s']", "custom-bucket", "custom-object-id"));
                            }
                        }
                    }
                }

                @Nested
                @DisplayName("Given a custom id resolver that cannot resolve the bucket")
                class CustomIdResolverThatCannotResolveBucket {
                    @Nested
                    @DisplayName("Given the default bucket is not set")
                    class DefaultBucketIsNotSet {
                        @Nested
                        @DisplayName("When called with an entity")
                        class CalledWithEntity {
                            @BeforeEach
                            void setUp() {
                                resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                loader = mock(ResourceLoader.class);
                                placementService = mock(PlacementService.class);
                                client = mock(S3Client.class);
                                defaultBucket = null;

                                context.registerBean("s3Client", S3Client.class, () -> client);
                                context = new GenericApplicationContext();
                                context.refresh();

                                defaultBucket = null;

                                entity = new SingleContentFields();
                                entity.setContentId(SAMPLE_CONTENT_ID);

                                placementService = new PlacementServiceImpl();
                                S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
                                placementService.addConverter(CustomContentId.class, S3ObjectId.class,
                                    entity -> objectIdWithNullBucket(entity.objectId()));

                                s3StoreImpl = new DefaultS3StoreImpl<>(context, loader, null, placementService, client, null);

                                try {
                                    r = s3StoreImpl.getResource(entity);
                                } catch (Exception e) {
                                    DefaultS3StoreImplTest.this.e = e;
                                }
                            }

                            @Test
                            void shouldThrowException() {
                                assertThat(e).isInstanceOf(ConversionFailedException.class);
                            }
                        }
                    }
                }
            }

            @Nested
            class GetResourceWithPropertyPath {
                @Nested
                @DisplayName("Given the default associative store ID resolver")
                class DefaultAssociativeStoreIdResolver {
                    @Nested
                    @DisplayName("Given a default bucket")
                    class DefaultBucket {
                        @Nested
                        @DisplayName("When called with entity that doesnt have bucket value")
                        class CalledWithEntityThatDoesntHaveBucketValue {
                            @BeforeEach
                            void setUp() {
                                resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                loader = mock(ResourceLoader.class);
                                placementService = mock(PlacementService.class);
                                client = mock(S3Client.class);
                                defaultBucket = null;

                                context.registerBean("s3Client", S3Client.class, () -> client);
                                context = new GenericApplicationContext();
                                context.refresh();

                                defaultBucket = "default-defaultBucket";

                                entity = new SingleContentFields();
                                entity.setContentId(SAMPLE_CONTENT_ID);

                                placementService = new PlacementServiceImpl();
                                S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
                                placementService.addConverter(S3ObjectId.class, String.class,
                                    source -> "/" + source.getKey().replace("-", "/"));

                                SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver(client);
                                s3Protocol.afterPropertiesSet();
                                loader = new DefaultResourceLoader();
                                ((DefaultResourceLoader) loader).addProtocolResolver(s3Protocol);

                                s3StoreImpl = new DefaultS3StoreImpl<>(context, loader, null, placementService, client, null);

                                try {
                                    r = s3StoreImpl.getResource(entity, PropertyPath.from("content"));
                                } catch (Exception e) {
                                    DefaultS3StoreImplTest.this.e = e;
                                }
                            }

                            @Test
                            void shouldFetchResource() {
                                assertThat(e).isNull();
                                assertThat(r).isInstanceOf(S3StoreResource.class);
                                assertThat(((S3StoreResource) r).getClient()).isEqualTo(client);
                                assertThat(r.getDescription()).isEqualTo(format("Amazon s3 resource [bucket='%s' and object='%s']", "default-defaultBucket", "12345/67890"));
                            }
                        }

                        @Nested
                        @DisplayName("When called with an entity that has an bucket value")
                        class CalledWithEntityThatHasBucketValue {
                            @BeforeEach
                            void setUp() {
                                resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                loader = mock(ResourceLoader.class);
                                placementService = mock(PlacementService.class);
                                client = mock(S3Client.class);
                                defaultBucket = null;

                                context.registerBean("s3Client", S3Client.class, () -> client);
                                context = new GenericApplicationContext();
                                context.refresh();

                                defaultBucket = "default-defaultBucket";

                                entity = new TestEntityWithBucketAnnotation(
                                    "some-other-bucket");
                                entity.setContentId(SAMPLE_CONTENT_ID);

                                placementService = new PlacementServiceImpl();
                                S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

                                SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver(client);
                                s3Protocol.afterPropertiesSet();
                                loader = new DefaultResourceLoader();
                                ((DefaultResourceLoader) loader).addProtocolResolver(s3Protocol);

                                s3StoreImpl = new DefaultS3StoreImpl<>(context, loader, null, placementService, client, null);

                                try {
                                    r = s3StoreImpl.getResource(entity, PropertyPath.from("content"));
                                } catch (Exception e) {
                                    DefaultS3StoreImplTest.this.e = e;
                                }
                            }

                            @Test
                            void shouldFetchCorrectResource() {
                                assertThat(e).isNull();
                                assertThat(r).isInstanceOf(S3StoreResource.class);
                                assertThat(((S3StoreResource) r).getClient()).isEqualTo(client);
                                assertThat(r.getDescription()).isEqualTo(format("Amazon s3 resource [bucket='%s' and object='%s']", "some-other-bucket", SAMPLE_CONTENT_ID));
                            }
                        }

                        @Nested
                        @DisplayName("When called with an entity that has no associated resource")
                        class CalledWithEntityThatHasNoAssociatedResource {
                            @BeforeEach
                            void setUp() {
                                resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                loader = mock(ResourceLoader.class);
                                placementService = mock(PlacementService.class);
                                client = mock(S3Client.class);
                                defaultBucket = null;

                                context.registerBean("s3Client", S3Client.class, () -> client);
                                context = new GenericApplicationContext();
                                context.refresh();

                                defaultBucket = "default-defaultBucket";

                                entity = new SingleContentFields();

                                s3StoreImpl = new DefaultS3StoreImpl<>(context, loader, null, placementService, client, null);

                                try {
                                    r = s3StoreImpl.getResource(entity, PropertyPath.from("content"));
                                } catch (Exception e) {
                                    DefaultS3StoreImplTest.this.e = e;
                                }
                            }

                            @Test
                            void shouldReturnNull() {
                                assertThat(r).isNull();
                                assertThat(e).isNull();
                            }
                        }
                    }
                }

                @Nested
                @DisplayName("Given a custom id resolver")
                class CustomIdResolver {
                    @Nested
                    @DisplayName("Given a default bucket")
                    class DefaultBucket {
                        @Nested
                        @DisplayName("When called with an entity")
                        class CalledWithEntity {
                            @BeforeEach
                            void setUp() {
                                resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                loader = mock(ResourceLoader.class);
                                placementService = mock(PlacementService.class);
                                client = mock(S3Client.class);
                                defaultBucket = null;

                                context.registerBean("s3Client", S3Client.class, () -> client);
                                context = new GenericApplicationContext();
                                context.refresh();

                                defaultBucket = "default-defaultBucket";

                                entity = new SingleContentFields();
                                entity.setContentId(SAMPLE_CONTENT_ID);

                                placementService = new PlacementServiceImpl();
                                S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

                                // Converter that matches Entity and content ID types. Expected to be invoked.
                                // Converter<ContentPropertyInfo<SingleContentFields, String>, S3ObjectId> instead of Converter<SingleContentFields, S3ObjectId> for #getResource
                                placementService.addConverter(
                                    new MatchingSingleContentFieldsStringContentPropertyInfoConverter());
                                // Converter that does not match content ID type. Should not be invoked.
                                placementService.addConverter(new NonMatchingUuidContentPropertyInfoConverter());
                                // Converter that does not match Entity type. Should not be invoked.
                                placementService.addConverter(new NonMatchingBucketEntityContentPropertyInfoConverter());

                                SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver(client);
                                s3Protocol.afterPropertiesSet();
                                loader = new DefaultResourceLoader();
                                ((DefaultResourceLoader) loader).addProtocolResolver(s3Protocol);

                                s3StoreImpl = new DefaultS3StoreImpl<>(context, loader, null, placementService, client, null);

                                try {
                                    r = s3StoreImpl.getResource(entity, PropertyPath.from("content"));
                                } catch (Exception e) {
                                    DefaultS3StoreImplTest.this.e = e;
                                }
                            }

                            @Test
                            void shouldFetchResource() {
                                assertThat(e).isNull();
                                assertThat(r).isInstanceOf(S3StoreResource.class);
                                assertThat(((S3StoreResource) r).getClient()).isEqualTo(client);
                                assertThat(r.getDescription()).isEqualTo(format("Amazon s3 resource [bucket='%s' and object='%s']", "custom-bucket", "test-entity/custom-object-id-string-based"));
                            }
                        }
                    }
                }

                @Nested
                @DisplayName("Given a custom id resolver that cannot resolve the bucket")
                class CustomIdResolverThatCannotResolveBucket {
                    @Nested
                    @DisplayName("Given the default bucket is not set")
                    class DefaultBucketIsNotSet {
                        @Nested
                        @DisplayName("When called with an entity")
                        class CalledWithEntity {
                            @BeforeEach
                            void setUp() {
                                resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                loader = mock(ResourceLoader.class);
                                placementService = mock(PlacementService.class);
                                client = mock(S3Client.class);
                                defaultBucket = null;

                                context.registerBean("s3Client", S3Client.class, () -> client);
                                context = new GenericApplicationContext();
                                context.refresh();

                                defaultBucket = null;

                                entity = new SingleContentFields();
                                entity.setContentId(SAMPLE_CONTENT_ID);

                                placementService = new PlacementServiceImpl();
                                S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
                                placementService.addConverter(new NullBucketContentPropertyInfoConverter());

                                s3StoreImpl = new DefaultS3StoreImpl<>(context, loader, null, placementService, client, null);

                                try {
                                    r = s3StoreImpl.getResource(entity, PropertyPath.from("content"));
                                } catch (Exception e) {
                                    DefaultS3StoreImplTest.this.e = e;
                                }
                            }

                            @Test
                            void shouldThrowException() {
                                assertThat(e).isInstanceOf(ConversionFailedException.class);
                            }
                        }
                    }
                }
            }

            @Nested
            class Associate {
                @BeforeEach
                void setUp() {
                    resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                    loader = mock(ResourceLoader.class);
                    placementService = mock(PlacementService.class);
                    client = mock(S3Client.class);
                    defaultBucket = null;

                    context.registerBean("s3Client", S3Client.class, () -> client);
                    context = new GenericApplicationContext();
                    context.refresh();

                    entity = new SingleContentFields();

                    s3StoreImpl = new DefaultS3StoreImpl<>(context, loader, null, placementService, client, null);

                    s3StoreImpl.associate(entity, SAMPLE_CONTENT_ID);
                }

                @Test
                void shouldSetEntitySContentIDAttribute() {
                    assertThat(entity.getContentId()).isEqualTo(SAMPLE_CONTENT_ID);
                }
            }

            @Nested
            class Unassociate {
                @BeforeEach
                void setUp() {
                    resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                    loader = mock(ResourceLoader.class);
                    placementService = mock(PlacementService.class);
                    client = mock(S3Client.class);
                    defaultBucket = null;

                    context.registerBean("s3Client", S3Client.class, () -> client);
                    context = new GenericApplicationContext();
                    context.refresh();

                    entity = new SingleContentFields();
                    entity.setContentId(SAMPLE_CONTENT_ID);

                    s3StoreImpl = new DefaultS3StoreImpl<>(context, loader, null, placementService, client, null);

                    s3StoreImpl.unassociate(entity);
                }

                @Test
                void shouldResetEntitySContentIDAttribute() {
                    assertThat(entity.getContentId()).isNull();
                }
            }
        }

        @Nested
        class ContentStoreScenario {
            @Nested
            class SetContent {
                @Nested
                @DisplayName("Given the default associative store ID resolver")
                class DefaultAssociativeStoreIdResolver {
                    @Nested
                    @DisplayName("Given a default bucket is set")
                    class DefaultBucketIsSet {
                        @Nested
                        @DisplayName("When the content already exists")
                        class ContentAlreadyExists {
                            @BeforeEach
                            void setUp() throws IOException {
                                resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                loader = mock(ResourceLoader.class);
                                placementService = mock(PlacementService.class);
                                client = mock(S3Client.class);
                                defaultBucket = null;

                                context.registerBean("s3Client", S3Client.class, () -> client);
                                context = new GenericApplicationContext();
                                context.refresh();

                                entity = new SingleContentFields();
                                content = new ByteArrayInputStream(
                                    "Hello content world!".getBytes());

                                defaultBucket = "default-defaultBucket";

                                entity.setContentId(SAMPLE_CONTENT_ID);

                                placementService = new PlacementServiceImpl();
                                S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

                                when(loader.getResource(endsWith(SAMPLE_CONTENT_ID))).thenReturn(resource);
                                output = mock(OutputStream.class);
                                when(resource.getOutputStream()).thenReturn(output);

                                when(resource.contentLength()).thenReturn(20L);

                                when(resource.exists()).thenReturn(true);

                                s3StoreImpl = spy(new DefaultS3StoreImpl<>(context, loader, null, placementService, client, null));

                                try {
                                    s3StoreImpl.setContent(entity, content);
                                } catch (Exception e) {
                                    DefaultS3StoreImplTest.this.e = e;
                                }
                            }

                            @Test
                            void shouldFetchResource() {
                                verify(loader).getResource(eq("s3://default-defaultBucket/" + SAMPLE_CONTENT_ID));
                            }

                            @Test
                            void shouldChangeContentLength() {
                                assertThat(entity.getContentLen()).isEqualTo(20L);
                            }

                            @Test
                            void shouldWriteToResourcesOutputStream() throws IOException {
                                verify(resource).getOutputStream();
                                verify(output, times(1)).write(any(byte[].class),
                                    eq(0), eq(20));
                            }

                            @Nested
                            @DisplayName("When the resource output stream throws an io exception")
                            class ResourceOutputStreamThrowsIOException {
                                @BeforeEach
                                void setUp() throws IOException {
                                    resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                    loader = mock(ResourceLoader.class);
                                    placementService = mock(PlacementService.class);
                                    client = mock(S3Client.class);
                                    defaultBucket = null;

                                    context.registerBean("s3Client", S3Client.class, () -> client);
                                    context = new GenericApplicationContext();
                                    context.refresh();

                                    entity = new SingleContentFields();
                                    content = new ByteArrayInputStream(
                                        "Hello content world!".getBytes());

                                    defaultBucket = "default-defaultBucket";

                                    entity.setContentId(SAMPLE_CONTENT_ID);

                                    placementService = new PlacementServiceImpl();
                                    S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

                                    when(loader.getResource(endsWith(SAMPLE_CONTENT_ID))).thenReturn(resource);
                                    output = mock(OutputStream.class);
                                    when(resource.getOutputStream()).thenReturn(output);

                                    when(resource.contentLength()).thenReturn(20L);

                                    when(resource.exists()).thenReturn(true);

                                    when(resource.getOutputStream()).thenThrow(new IOException("set-ioexception"));

                                    s3StoreImpl = spy(new DefaultS3StoreImpl<>(context, loader, null, placementService, client, null));

                                    try {
                                        s3StoreImpl.setContent(entity, content);
                                    } catch (Exception e) {
                                        DefaultS3StoreImplTest.this.e = e;
                                    }
                                }

                                @Test
                                void shouldThrowStoreAccessException() {
                                    assertThat(e).isInstanceOf(StoreAccessException.class);
                                    assertThat(e.getCause().getMessage()).isEqualTo("set-ioexception");
                                }
                            }
                        }

                        @Nested
                        @DisplayName("When the content does not already exist")
                        class ContentDoesNotAlreadyExist {
                            @BeforeEach
                            void setUp() throws IOException {
                                resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                loader = mock(ResourceLoader.class);
                                placementService = mock(PlacementService.class);
                                client = mock(S3Client.class);
                                defaultBucket = null;

                                context.registerBean("s3Client", S3Client.class, () -> client);
                                context = new GenericApplicationContext();
                                context.refresh();

                                entity = new SingleContentFields();
                                content = new ByteArrayInputStream(
                                    "Hello content world!".getBytes());

                                defaultBucket = "default-defaultBucket";

                                assertThat(entity.getContentId()).isNull();

                                placementService = new PlacementServiceImpl();
                                S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

                                when(loader.getResource(matches("^s3://.*[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"))).thenReturn(resource);
                                output = mock(OutputStream.class);
                                when(resource.getOutputStream()).thenReturn(output);

                                when(resource.contentLength()).thenReturn(20L);

                                File resourceFile = mock(File.class);
                                File parent = mock(File.class);

                                when(resource.getFile()).thenReturn(resourceFile);
                                when(resourceFile.getParentFile()).thenReturn(parent);

                                s3StoreImpl = spy(new DefaultS3StoreImpl<>(context, loader, null, placementService, client, null));

                                try {
                                    s3StoreImpl.setContent(entity, content);
                                } catch (Exception e) {
                                    DefaultS3StoreImplTest.this.e = e;
                                }
                            }

                            @Test
                            void shouldMakeNewUUID() {
                                assertThat(entity.getContentId()).isNotNull();
                            }

                            @Test
                            void shouldCreateNewResource() {
                                verify(loader).getResource(matches("^s3://.*[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"));
                            }

                            @Test
                            void shouldWriteToResourcesOutputStream() throws IOException {
                                verify(resource).getOutputStream();
                                verify(output, times(1)).write(any(byte[].class),
                                    eq(0), eq(20));
                            }
                        }

                        @Nested
                        @DisplayName("When s3throws an s3exception")
                        class S3ThrowsS3Exception {
                            @BeforeEach
                            void setUp() throws IOException {
                                resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                loader = mock(ResourceLoader.class);
                                placementService = mock(PlacementService.class);
                                client = mock(S3Client.class);
                                defaultBucket = null;

                                context.registerBean("s3Client", S3Client.class, () -> client);
                                context = new GenericApplicationContext();
                                context.refresh();

                                entity = new SingleContentFields();
                                content = new ByteArrayInputStream(
                                    "Hello content world!".getBytes());

                                defaultBucket = "default-defaultBucket";

                                assertThat(entity.getContentId()).isNull();

                                placementService = new PlacementServiceImpl();
                                S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

                                when(loader.getResource(matches("^s3://.*[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"))).thenReturn(resource);
                                output = mock(OutputStream.class);
                                when(resource.getOutputStream()).thenReturn(output);

                                doThrow(S3Exception.builder().message("no such upload").build()).when(output).close();

                                s3StoreImpl = spy(new DefaultS3StoreImpl<>(context, loader, null, placementService, client, null));

                                try {
                                    s3StoreImpl.setContent(entity, content);
                                } catch (Exception e) {
                                    DefaultS3StoreImplTest.this.e = e;
                                }
                            }

                            @Test
                            void shouldDoSomething() {
                                assertThat(e).isInstanceOf(S3Exception.class);
                            }
                        }
                    }
                }
            }

            @Nested
            class SetContentFromResource {
                @BeforeEach
                void setUp() {
                    resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                    loader = mock(ResourceLoader.class);
                    placementService = mock(PlacementService.class);
                    client = mock(S3Client.class);
                    defaultBucket = null;

                    context.registerBean("s3Client", S3Client.class, () -> client);
                    context = new GenericApplicationContext();
                    context.refresh();

                    entity = new SingleContentFields();
                    content = new ByteArrayInputStream("Hello content world!".getBytes());
                    r = new InputStreamResource(content);

                    s3StoreImpl = spy(new DefaultS3StoreImpl<>(context, loader, null, placementService, client, null));

                    try {
                        s3StoreImpl.setContent(entity, r);
                    } catch (Exception e) {
                        DefaultS3StoreImplTest.this.e = e;
                    }
                }

                @Test
                void shouldDelegate() {
                    verify(s3StoreImpl).setContent(eq(entity), eq(content));
                }

                @Nested
                @DisplayName("When the resource throws an io exception")
                class ResourceThrowsIOException {
                    @BeforeEach
                    void setUp() throws IOException {
                        resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                        loader = mock(ResourceLoader.class);
                        placementService = mock(PlacementService.class);
                        client = mock(S3Client.class);
                        defaultBucket = null;

                        context.registerBean("s3Client", S3Client.class, () -> client);
                        context = new GenericApplicationContext();
                        context.refresh();

                        entity = new SingleContentFields();
                        content = new ByteArrayInputStream("Hello content world!".getBytes());
                        r = new InputStreamResource(content);

                        r = mock(Resource.class);
                        when(r.getInputStream()).thenThrow(new IOException("setContent badness"));

                        s3StoreImpl = spy(new DefaultS3StoreImpl<>(context, loader, null, placementService, client, null));

                        try {
                            s3StoreImpl.setContent(entity, r);
                        } catch (Exception e) {
                            DefaultS3StoreImplTest.this.e = e;
                        }
                    }

                    @Test
                    void shouldThrowStoreAccessException() {
                        assertThat(e).isInstanceOf(StoreAccessException.class);
                        assertThat(e.getCause().getMessage()).contains("setContent badness");
                    }
                }
            }

            @Nested
            class GetContent {
                @Nested
                @DisplayName("Given the default associative store ID resolver")
                class DefaultAssociativeStoreIdResolver {
                    @Nested
                    @DisplayName("Given a default bucket is set")
                    class DefaultBucketIsSet {
                        @Nested
                        @DisplayName("When called with an entity")
                        class CalledWithEntity {
                            @Nested
                            @DisplayName("And the resource already exists")
                            class ResourceAlreadyExists {
                                @BeforeEach
                                void setUp() throws IOException {
                                    resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                    loader = mock(ResourceLoader.class);
                                    placementService = mock(PlacementService.class);
                                    client = mock(S3Client.class);
                                    defaultBucket = null;

                                    context.registerBean("s3Client", S3Client.class, () -> client);
                                    context = new GenericApplicationContext();
                                    context.refresh();

                                    defaultBucket = "default-defaultBucket";

                                    entity = new SingleContentFields();
                                    content = mock(InputStream.class);
                                    entity.setContentId(SAMPLE_CONTENT_ID);

                                    placementService = new PlacementServiceImpl();
                                    S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

                                    when(loader.getResource(matches("^s3://default-defaultBucket/" + SAMPLE_CONTENT_ID))).thenReturn(resource);
                                    when(resource.getInputStream()).thenReturn(content);

                                    when(resource.exists()).thenReturn(true);

                                    s3StoreImpl = spy(new DefaultS3StoreImpl<>(context, loader, null, placementService, client, null));

                                    try {
                                        result = s3StoreImpl.getContent(entity);
                                    } catch (Exception e) {
                                        DefaultS3StoreImplTest.this.e = e;
                                    }
                                }

                                @Test
                                void shouldFetchResource() {
                                    verify(loader).getResource(eq("s3://default-defaultBucket/" + SAMPLE_CONTENT_ID));
                                }

                                @Test
                                void shouldGetContent() {
                                    assertThat(result).isEqualTo(content);
                                }

                                @Nested
                                @DisplayName("When the resource input stream throws an io exception")
                                class ResourceInputStreamThrowsIOException {
                                    @BeforeEach
                                    void setUp() throws IOException {
                                        resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                        loader = mock(ResourceLoader.class);
                                        placementService = mock(PlacementService.class);
                                        client = mock(S3Client.class);
                                        defaultBucket = null;

                                        context.registerBean("s3Client", S3Client.class, () -> client);
                                        context = new GenericApplicationContext();
                                        context.refresh();

                                        defaultBucket = "default-defaultBucket";

                                        entity = new SingleContentFields();
                                        content = mock(InputStream.class);
                                        entity.setContentId(SAMPLE_CONTENT_ID);

                                        placementService = new PlacementServiceImpl();
                                        S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

                                        when(loader.getResource(matches("^s3://default-defaultBucket/" + SAMPLE_CONTENT_ID))).thenReturn(resource);
                                        when(resource.getInputStream()).thenReturn(content);

                                        when(resource.exists()).thenReturn(true);

                                        when(resource.getInputStream()).thenThrow(new IOException("get-ioexception"));

                                        s3StoreImpl = spy(new DefaultS3StoreImpl<>(context, loader, null, placementService, client, null));

                                        try {
                                            result = s3StoreImpl.getContent(entity);
                                        } catch (Exception e) {
                                            DefaultS3StoreImplTest.this.e = e;
                                        }
                                    }

                                    @Test
                                    void shouldThrowStoreAccessException() {
                                        assertThat(e).isInstanceOf(StoreAccessException.class);
                                        assertThat(e.getCause().getMessage()).isEqualTo("get-ioexception");
                                    }
                                }
                            }

                            @Nested
                            @DisplayName("And the resource doesnt exist")
                            class ResourceDoesntExist {
                                @BeforeEach
                                void setUp() throws IOException {
                                    resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                    loader = mock(ResourceLoader.class);
                                    placementService = mock(PlacementService.class);
                                    client = mock(S3Client.class);
                                    defaultBucket = null;

                                    context.registerBean("s3Client", S3Client.class, () -> client);
                                    context = new GenericApplicationContext();
                                    context.refresh();

                                    defaultBucket = "default-defaultBucket";

                                    entity = new SingleContentFields();
                                    content = mock(InputStream.class);
                                    entity.setContentId(SAMPLE_CONTENT_ID);

                                    placementService = new PlacementServiceImpl();
                                    S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

                                    when(loader.getResource(matches("^s3://default-defaultBucket/" + SAMPLE_CONTENT_ID))).thenReturn(resource);
                                    when(resource.getInputStream()).thenReturn(content);

                                    nonExistentResource = mock(WritableResource.class);
                                    when(resource.exists()).thenReturn(true);

                                    when(loader.getResource(endsWith(SAMPLE_CONTENT_ID))).thenReturn(nonExistentResource);

                                    s3StoreImpl = spy(new DefaultS3StoreImpl<>(context, loader, null, placementService, client, null));

                                    try {
                                        result = s3StoreImpl.getContent(entity);
                                    } catch (Exception e) {
                                        DefaultS3StoreImplTest.this.e = e;
                                    }
                                }

                                @Test
                                void shouldFetchResource() {
                                    verify(loader).getResource(eq("s3://default-defaultBucket/" + SAMPLE_CONTENT_ID));
                                }

                                @Test
                                void shouldNotFindContent() {
                                    assertThat(result).isNull();
                                }
                            }

                            @Nested
                            class WithNullContentId {
                                @BeforeEach
                                void setUp() throws IOException {
                                    resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                    loader = mock(ResourceLoader.class);
                                    placementService = mock(PlacementService.class);
                                    client = mock(S3Client.class);
                                    defaultBucket = null;

                                    context.registerBean("s3Client", S3Client.class, () -> client);
                                    context = new GenericApplicationContext();
                                    context.refresh();

                                    defaultBucket = "default-defaultBucket";

                                    entity = new SingleContentFields();
                                    content = mock(InputStream.class);
                                    entity.setContentId(SAMPLE_CONTENT_ID);

                                    placementService = new PlacementServiceImpl();
                                    S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

                                    when(loader.getResource(matches("^s3://default-defaultBucket/" + SAMPLE_CONTENT_ID))).thenReturn(resource);
                                    when(resource.getInputStream()).thenReturn(content);

                                    entity.setContentId(null);

                                    s3StoreImpl = spy(new DefaultS3StoreImpl<>(context, loader, null, placementService, client, null));

                                    try {
                                        result = s3StoreImpl.getContent(entity);
                                    } catch (Exception e) {
                                        DefaultS3StoreImplTest.this.e = e;
                                    }
                                }

                                @Test
                                void shouldReturnNull() {
                                    assertThat(result).isNull();
                                    assertThat(e).isNull();
                                }
                            }
                        }
                    }
                }
            }

            @Nested
            class UnsetContent {
                @Nested
                @DisplayName("Given the default associative store ID resolver")
                class DefaultAssociativeStoreIdResolver {
                    @Nested
                    @DisplayName("Given a default bucket is set")
                    class DefaultBucketIsSet {
                        @Nested
                        @DisplayName("When called with an entity")
                        class CalledWithEntity {
                            @Nested
                            @DisplayName("And the content exists")
                            class ContentExists {
                                @BeforeEach
                                void setUp() {
                                    resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                    loader = mock(ResourceLoader.class);
                                    placementService = mock(PlacementService.class);
                                    client = mock(S3Client.class);
                                    defaultBucket = null;

                                    context.registerBean("s3Client", S3Client.class, () -> client);
                                    context = new GenericApplicationContext();
                                    context.refresh();

                                    defaultBucket = "default-defaultBucket";

                                    entity = new SingleContentFields();
                                    entity.setContentId(SAMPLE_CONTENT_ID);
                                    entity.setContentLen(100L);
                                    resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));

                                    placementService = new PlacementServiceImpl();
                                    S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

                                    when(loader.getResource(endsWith(SAMPLE_CONTENT_ID))).thenReturn(resource);
                                    when(resource.exists()).thenReturn(true);

                                    s3StoreImpl = spy(new DefaultS3StoreImpl<>(context, loader, null, placementService, client, null));

                                    try {
                                        s3StoreImpl.unsetContent(entity);
                                    } catch (Exception e) {
                                        DefaultS3StoreImplTest.this.e = e;
                                    }
                                }

                                @Test
                                void shouldFetchResource() {
                                    verify(loader).getResource(eq("s3://default-defaultBucket/" + SAMPLE_CONTENT_ID));
                                }

                                @Nested
                                @DisplayName("When the property has a dedicated content ID field")
                                class PropertyHasDedicatedContentIdField {
                                    @BeforeEach
                                    void setUp() {
                                        resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                        loader = mock(ResourceLoader.class);
                                        placementService = mock(PlacementService.class);
                                        client = mock(S3Client.class);
                                        defaultBucket = null;

                                        context.registerBean("s3Client", S3Client.class, () -> client);
                                        context = new GenericApplicationContext();
                                        context.refresh();

                                        defaultBucket = "default-defaultBucket";

                                        entity = new SingleContentFields();
                                        entity.setContentId(SAMPLE_CONTENT_ID);
                                        entity.setContentLen(100L);
                                        resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));

                                        placementService = new PlacementServiceImpl();
                                        S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

                                        when(loader.getResource(endsWith(SAMPLE_CONTENT_ID))).thenReturn(resource);
                                        when(resource.exists()).thenReturn(true);

                                        s3StoreImpl = spy(new DefaultS3StoreImpl<>(context, loader, null, placementService, client, null));

                                        try {
                                            s3StoreImpl.unsetContent(entity);
                                        } catch (Exception e) {
                                            DefaultS3StoreImplTest.this.e = e;
                                        }
                                    }

                                    @Test
                                    void shouldResetMetadata() {
                                        assertThat(entity.getContentId()).isNull();
                                        assertThat(entity.getContentLen()).isNull();
                                    }
                                }

                                @Nested
                                @DisplayName("When the property s content ID field also is the jakarta persistence id field")
                                class PropertySContentIdFieldAlsoIsJakartaPersistenceIdField {
                                    @BeforeEach
                                    void setUp() {
                                        resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                        loader = mock(ResourceLoader.class);
                                        placementService = mock(PlacementService.class);
                                        client = mock(S3Client.class);
                                        defaultBucket = null;

                                        context.registerBean("s3Client", S3Client.class, () -> client);
                                        context = new GenericApplicationContext();
                                        context.refresh();

                                        defaultBucket = "default-defaultBucket";

                                        entity = new SingleContentFields();
                                        entity.setContentId(SAMPLE_CONTENT_ID);
                                        entity.setContentLen(100L);
                                        resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));

                                        placementService = new PlacementServiceImpl();
                                        S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

                                        when(loader.getResource(endsWith(SAMPLE_CONTENT_ID))).thenReturn(resource);
                                        when(resource.exists()).thenReturn(true);

                                        entity = new SharedIdJakartaJpaEntity();
                                        entity.setContentId(SAMPLE_CONTENT_ID);

                                        s3StoreImpl = spy(new DefaultS3StoreImpl<>(context, loader, null, placementService, client, null));

                                        try {
                                            s3StoreImpl.unsetContent(entity);
                                        } catch (Exception e) {
                                            DefaultS3StoreImplTest.this.e = e;
                                        }
                                    }

                                    @Test
                                    void shouldNotResetContentIdMetadata() {
                                        assertThat(entity.getContentId()).isEqualTo(SAMPLE_CONTENT_ID);
                                        assertThat(entity.getContentLen()).isNull();
                                    }
                                }

                                @Nested
                                @DisplayName("When the property s content ID field also is the spring id field")
                                class PropertySContentIdFieldAlsoIsSpringIdField {
                                    @BeforeEach
                                    void setUp() {
                                        resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                        loader = mock(ResourceLoader.class);
                                        placementService = mock(PlacementService.class);
                                        client = mock(S3Client.class);
                                        defaultBucket = null;

                                        context.registerBean("s3Client", S3Client.class, () -> client);
                                        context = new GenericApplicationContext();
                                        context.refresh();

                                        defaultBucket = "default-defaultBucket";

                                        entity = new SingleContentFields();
                                        entity.setContentId(SAMPLE_CONTENT_ID);
                                        entity.setContentLen(100L);
                                        resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));

                                        placementService = new PlacementServiceImpl();
                                        S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

                                        when(loader.getResource(endsWith(SAMPLE_CONTENT_ID))).thenReturn(resource);
                                        when(resource.exists()).thenReturn(true);

                                        entity = new SharedIdSpringJpaEntity();
                                        entity.setContentId(SAMPLE_CONTENT_ID);

                                        s3StoreImpl = spy(new DefaultS3StoreImpl<>(context, loader, null, placementService, client, null));

                                        try {
                                            s3StoreImpl.unsetContent(entity);
                                        } catch (Exception e) {
                                            DefaultS3StoreImplTest.this.e = e;
                                        }
                                    }

                                    @Test
                                    void shouldNotResetContentIdMetadata() {
                                        assertThat(entity.getContentId()).isEqualTo(SAMPLE_CONTENT_ID);
                                        assertThat(entity.getContentLen()).isNull();
                                    }
                                }
                            }

                            @Nested
                            @DisplayName("And the content doesnt exist")
                            class ContentDoesntExist {
                                @BeforeEach
                                void setUp() {
                                    resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                    loader = mock(ResourceLoader.class);
                                    placementService = mock(PlacementService.class);
                                    client = mock(S3Client.class);
                                    defaultBucket = null;

                                    context.registerBean("s3Client", S3Client.class, () -> client);
                                    context = new GenericApplicationContext();
                                    context.refresh();

                                    defaultBucket = "default-defaultBucket";

                                    entity = new SingleContentFields();
                                    entity.setContentId(SAMPLE_CONTENT_ID);
                                    entity.setContentLen(100L);
                                    resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));

                                    placementService = new PlacementServiceImpl();
                                    S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

                                    nonExistentResource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                    when(loader.getResource(endsWith(SAMPLE_CONTENT_ID))).thenReturn(nonExistentResource);
                                    when(nonExistentResource.exists()).thenReturn(false);

                                    s3StoreImpl = spy(new DefaultS3StoreImpl<>(context, loader, null, placementService, client, null));

                                    try {
                                        s3StoreImpl.unsetContent(entity);
                                    } catch (Exception e) {
                                        DefaultS3StoreImplTest.this.e = e;
                                    }
                                }

                                @Test
                                void shouldFetchResource() {
                                    verify(loader).getResource(eq("s3://default-defaultBucket/" + SAMPLE_CONTENT_ID));
                                }

                                @Test
                                void shouldUnsetContent() {
                                    verify(client, never()).deleteObject(any(DeleteObjectRequest.class));
                                    assertThat(entity.getContentId()).isNull();
                                    assertThat(entity.getContentLen()).isNull();
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
