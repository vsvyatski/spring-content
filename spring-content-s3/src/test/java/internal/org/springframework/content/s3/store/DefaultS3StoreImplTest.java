package internal.org.springframework.content.s3.store;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;

import static java.lang.String.format;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.config.ContentPropertyInfo;
import org.springframework.content.commons.io.RangeableResource;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.content.commons.utils.PlacementServiceImpl;
import org.springframework.content.s3.Bucket;
import org.springframework.content.s3.S3ObjectId;
import org.springframework.content.s3.config.MultiTenantS3ClientProvider;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;

import internal.org.springframework.content.s3.config.S3StoreConfiguration;
import internal.org.springframework.content.s3.io.S3StoreResource;
import internal.org.springframework.content.s3.io.SimpleStorageProtocolResolver;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

public class DefaultS3StoreImplTest {

	private DefaultS3StoreImpl<ContentProperty, String> s3StoreImpl;
	private DefaultS3StoreImpl<ContentProperty, S3ObjectId> s3ObjectIdBasedStore;
	private DefaultS3StoreImpl<ContentProperty, CustomContentId> customS3ContentIdBasedStore;

	private GenericApplicationContext context = new GenericApplicationContext();
	private ResourceLoader loader;
	private PlacementService placementService;
	private S3Client client, client2;

	private MultiTenantS3ClientProvider clientProvider;

	private String defaultBucket;

	private CustomContentId customId;
	private ContentProperty entity;

	private String id;
	private WritableResource resource;
	private Resource r, nonExistentResource;
	private InputStream content;
	private OutputStream output;
	private File parent;
	private InputStream result;
	private Exception e;

	
    @Nested
    class DefaultS3StoreImplCases {
        @Nested
        class Store {
            @Nested
            class GetResource {
                @Nested
                class GivenTheStoreSIDIsAnS3ObjectIdType {
                    @BeforeEach
                    void setUp() {
                        resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                        loader = mock(ResourceLoader.class);
                        placementService = mock(PlacementService.class);
                        client = mock(S3Client.class);
                        defaultBucket = null;

                        context.registerBean("s3Client", S3Client.class, new Supplier() {

                                       @Override
                                       public Object get() {
                                           return client;
                                       }
                        }, new BeanDefinitionCustomizer[]{});
                        context = new GenericApplicationContext();
                        context.refresh();

                        placementService = new PlacementServiceImpl();
                        S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
                        placementService.addConverter(new Converter<String, String>() {
                        	@Override
                        	public String convert(String source) {
                        		return "/some/object/id";
                        	}
                        });

                        SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver(client);
                        s3Protocol.afterPropertiesSet();
                        loader = new DefaultResourceLoader();
                        ((DefaultResourceLoader)loader).addProtocolResolver(s3Protocol);

                        s3ObjectIdBasedStore = new DefaultS3StoreImpl<>(context, loader, null, placementService, client, null);

                        try {
                        	r = s3ObjectIdBasedStore.getResource(new S3ObjectId("some-defaultBucket", "some-object-id"));
                        } catch (Exception e) {
                        	DefaultS3StoreImplTest.this.e = e;
                        }
                    }

                    @Test
                    void shouldReturnTheResource() {
                        assertThat(e).isNull();
                        assertThat(r).isInstanceOf(S3StoreResource.class);
                        assertThat(((S3StoreResource)r).getClient()).isEqualTo(client);
                        assertThat(r.getDescription()).isEqualTo(format("Amazon s3 resource [bucket='%s' and object='%s']","some-defaultBucket", "some/object/id"));
                    }

                }

                @Nested
                class GivenTheStoreSIDIsACustomIDType {
                    @Nested
                    class GivenADefaultBucketIsSet {
                        @Nested
                        class GivenTheResolverIsCreatedWithTheStaticConstructorFunction {
                            @Nested
                            class GivenAnID {
                                @BeforeEach
                                void setUp() {
                                    resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                    loader = mock(ResourceLoader.class);
                                    placementService = mock(PlacementService.class);
                                    client = mock(S3Client.class);
                                    defaultBucket = null;

                                    context.registerBean("s3Client", S3Client.class, new Supplier() {

                                                   @Override
                                                   public Object get() {
                                                       return client;
                                                   }
                                    }, new BeanDefinitionCustomizer[]{});
                                    context = new GenericApplicationContext();
                                    context.refresh();

                                    defaultBucket = "default-customer";

                                    placementService = new PlacementServiceImpl();
                                    S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
                                    placementService.addConverter(new Converter<CustomContentId, S3ObjectId>() {

                                                                  @Override
                                                                  public S3ObjectId convert(CustomContentId entity) {
                                                                      return new S3ObjectId(entity.getCustomer(), entity.getObjectId());
                                                                  }
                                    });

                                    SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver(client);
                                    s3Protocol.afterPropertiesSet();
                                    loader = new DefaultResourceLoader();
                                    ((DefaultResourceLoader)loader).addProtocolResolver(s3Protocol);

                                    customId = new CustomContentId(
                                    		"some-customer",
                                    		"some-object-id");

                                    customS3ContentIdBasedStore = new DefaultS3StoreImpl<>(context, loader, null, placementService, client, null);

                                    try {
                                    	r = customS3ContentIdBasedStore.getResource(customId);
                                    }
                                    catch (Exception e) {
                                    	DefaultS3StoreImplTest.this.e = e;
                                    }
                                }

                                @Test
                                void shouldFetchTheResource() {
                                    assertThat(e).isNull();
                                    assertThat(r).isInstanceOf(S3StoreResource.class);
                                    assertThat(((S3StoreResource)r).getClient()).isEqualTo(client);
                                    assertThat(r.getDescription()).isEqualTo(format("Amazon s3 resource [bucket='%s' and object='%s']","some-customer", "some-object-id"));
                                }

                            }

                        }

                    }

                    @Nested
                    class GivenADefaultBucketIsNotSet {
                        @Nested
                        class GivenAResolverThatDoesNotValidate {
                            @Nested
                            class WhenCalledWithAnIDThatDoesnTSpecifyABucketEither {
                                @BeforeEach
                                void setUp() {
                                    resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                    loader = mock(ResourceLoader.class);
                                    placementService = mock(PlacementService.class);
                                    client = mock(S3Client.class);
                                    defaultBucket = null;

                                    context.registerBean("s3Client", S3Client.class, new Supplier() {

                                                   @Override
                                                   public Object get() {
                                                       return client;
                                                   }
                                    }, new BeanDefinitionCustomizer[]{});
                                    context = new GenericApplicationContext();
                                    context.refresh();

                                    defaultBucket = null;

                                    placementService = new PlacementServiceImpl();
                                    S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

                                    customId = new CustomContentId(null,"some-object-id");

                                    customS3ContentIdBasedStore = new DefaultS3StoreImpl<>(context, loader, null, placementService, client, null);

                                    try {
                                    	r = customS3ContentIdBasedStore.getResource(customId);
                                    }
                                    catch (Exception e) {
                                    	DefaultS3StoreImplTest.this.e = e;
                                    }
                                }

                                @Test
                                void shouldThrowAnError() {
                                    assertThat(e).isInstanceOf(ConversionFailedException.class);
                                }

                            }

                        }

                    }

                }

                @Nested
                class GivenAMultiTenantConfiguration {
                    @BeforeEach
                    void setUp() {
                        resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                        loader = mock(ResourceLoader.class);
                        placementService = mock(PlacementService.class);
                        client = mock(S3Client.class);
                        defaultBucket = null;

                        context.registerBean("s3Client", S3Client.class, new Supplier() {

                                       @Override
                                       public Object get() {
                                           return client;
                                       }
                        }, new BeanDefinitionCustomizer[]{});
                        context = new GenericApplicationContext();
                        context.refresh();

                        client2 = mock(S3Client.class);
                        clientProvider = new MultiTenantS3ClientProvider() {
                        	@Override
                        	public S3Client getS3Client() {
                        		return client2;
                        	};
                        };

                        placementService = new PlacementServiceImpl();
                        S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
                        s3ObjectIdBasedStore = new DefaultS3StoreImpl<>(context, loader, null, placementService, client, clientProvider);

                        try {
                        	r = s3ObjectIdBasedStore.getResource(new S3ObjectId("some-bucket", "some-object-id"));
                        }
                        catch (Exception e) {
                        	DefaultS3StoreImplTest.this.e = e;
                        }
                    }

                    @Test
                    void shouldFetchTheResourceUsingTheCorrectClient() {
                        assertThat(e).isNull();
                        assertThat(r).isInstanceOf(S3StoreResource.class);
                        assertThat(((S3StoreResource)r).getClient()).isEqualTo(client2);
                        assertThat(r.getDescription()).isEqualTo(format("Amazon s3 resource [bucket='%s' and object='%s']","some-bucket", "some-object-id"));
                    }

                }

            }

        }

        @Nested
        class AssociativeStore {
            @Nested
            class GetResource {
                @Nested
                class GivenTheDefaultAssociativeStoreIdResolver {
                    @Nested
                    class GivenADefaultBucket {
                        @Nested
                        class WhenCalledWithAnEntityThatDoesnTHaveAnBucketValue {
                            @BeforeEach
                            void setUp() {
                                resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                loader = mock(ResourceLoader.class);
                                placementService = mock(PlacementService.class);
                                client = mock(S3Client.class);
                                defaultBucket = null;

                                context.registerBean("s3Client", S3Client.class, new Supplier() {

                                               @Override
                                               public Object get() {
                                                   return client;
                                               }
                                }, new BeanDefinitionCustomizer[]{});
                                context = new GenericApplicationContext();
                                context.refresh();

                                defaultBucket = "default-defaultBucket";

                                entity = new TestEntity("12345-67890");

                                placementService = new PlacementServiceImpl();
                                                          S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
                                placementService.addConverter(new Converter<S3ObjectId, String>() {
                                                              @Override
                                                              public String convert(S3ObjectId source) {
                                                                  return "/" + source.getKey().replaceAll("-", "/");
                                                              }
                                                          });

                                SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver(client);
                                s3Protocol.afterPropertiesSet();
                                loader = new DefaultResourceLoader();
                                ((DefaultResourceLoader)loader).addProtocolResolver(s3Protocol);

                                s3StoreImpl = new DefaultS3StoreImpl<ContentProperty, String>(context,loader,null,placementService,client,null);

                                try {
                                	r = s3StoreImpl.getResource(entity);
                                }
                                catch (Exception e) {
                                	DefaultS3StoreImplTest.this.e = e;
                                }
                            }

                            @Test
                            void shouldFetchTheResource() {
                                assertThat(e).isNull();
                                assertThat(r).isInstanceOf(S3StoreResource.class);
                                assertThat(((S3StoreResource)r).getClient()).isEqualTo(client);
                                assertThat(r.getDescription()).isEqualTo(format("Amazon s3 resource [bucket='%s' and object='%s']","default-defaultBucket", "12345/67890"));
                            }

                        }

                        @Nested
                        class WhenCalledWithAnEntityThatHasAnBucketValue {
                            @BeforeEach
                            void setUp() {
                                resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                loader = mock(ResourceLoader.class);
                                placementService = mock(PlacementService.class);
                                client = mock(S3Client.class);
                                defaultBucket = null;

                                context.registerBean("s3Client", S3Client.class, new Supplier() {

                                               @Override
                                               public Object get() {
                                                   return client;
                                               }
                                }, new BeanDefinitionCustomizer[]{});
                                context = new GenericApplicationContext();
                                context.refresh();

                                defaultBucket = "default-defaultBucket";

                                entity = new TestEntityWithBucketAnnotation(
                                		"some-other-bucket");
                                entity.setContentId("12345-67890");

                                                          placementService = new PlacementServiceImpl();
                                                          S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

                                SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver(client);
                                s3Protocol.afterPropertiesSet();
                                loader = new DefaultResourceLoader();
                                ((DefaultResourceLoader)loader).addProtocolResolver(s3Protocol);

                                s3StoreImpl = new DefaultS3StoreImpl<ContentProperty, String>(context,loader,null,placementService,client,null);

                                try {
                                	r = s3StoreImpl.getResource(entity);
                                }
                                catch (Exception e) {
                                	DefaultS3StoreImplTest.this.e = e;
                                }
                            }

                            @Test
                            void shouldFetchTheCorrectResource() {
                                assertThat(e).isNull();
                                assertThat(r).isInstanceOf(S3StoreResource.class);
                                assertThat(((S3StoreResource)r).getClient()).isEqualTo(client);
                                assertThat(r.getDescription()).isEqualTo(format("Amazon s3 resource [bucket='%s' and object='%s']","some-other-bucket", "12345-67890"));
                            }

                        }

                        @Nested
                        class WhenCalledWithAnEntityThatHasNoAssociatedResource {
                            @BeforeEach
                            void setUp() {
                                resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                loader = mock(ResourceLoader.class);
                                placementService = mock(PlacementService.class);
                                client = mock(S3Client.class);
                                defaultBucket = null;

                                context.registerBean("s3Client", S3Client.class, new Supplier() {

                                               @Override
                                               public Object get() {
                                                   return client;
                                               }
                                }, new BeanDefinitionCustomizer[]{});
                                context = new GenericApplicationContext();
                                context.refresh();

                                defaultBucket = "default-defaultBucket";

                                entity = new TestEntity();

                                s3StoreImpl = new DefaultS3StoreImpl<ContentProperty, String>(context,loader,null,placementService,client,null);

                                try {
                                	r = s3StoreImpl.getResource(entity);
                                }
                                catch (Exception e) {
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
                class GivenACustomIdResolver {
                    @Nested
                    class GivenADefaultBucket {
                        @Nested
                        class WhenCalledWithAnEntity {
                            @BeforeEach
                            void setUp() {
                                resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                loader = mock(ResourceLoader.class);
                                placementService = mock(PlacementService.class);
                                client = mock(S3Client.class);
                                defaultBucket = null;

                                context.registerBean("s3Client", S3Client.class, new Supplier() {

                                               @Override
                                               public Object get() {
                                                   return client;
                                               }
                                }, new BeanDefinitionCustomizer[]{});
                                context = new GenericApplicationContext();
                                context.refresh();

                                defaultBucket = "default-defaultBucket";

                                entity = new TestEntity("12345-67890");

                                placementService = new PlacementServiceImpl();
                                S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
                                                          placementService.addConverter(new Converter<TestEntity, S3ObjectId>() {
                                                              @Override
                                                              public S3ObjectId convert(TestEntity source) {
                                                                  return new S3ObjectId( "custom-bucket", "custom-object-id");
                                                              }
                                                          });

                                SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver(client);
                                s3Protocol.afterPropertiesSet();
                                loader = new DefaultResourceLoader();
                                ((DefaultResourceLoader)loader).addProtocolResolver(s3Protocol);

                                s3StoreImpl = new DefaultS3StoreImpl<ContentProperty, String>(context,loader,null,placementService,client,null);

                                try {
                                	r = s3StoreImpl.getResource(entity);
                                }
                                catch (Exception e) {
                                	DefaultS3StoreImplTest.this.e = e;
                                }
                            }

                            @Test
                            void shouldFetchTheResource() {
                                assertThat(e).isNull();
                                assertThat(r).isInstanceOf(S3StoreResource.class);
                                assertThat(((S3StoreResource)r).getClient()).isEqualTo(client);
                                assertThat(r.getDescription()).isEqualTo(format("Amazon s3 resource [bucket='%s' and object='%s']","custom-bucket", "custom-object-id"));
                            }

                        }

                    }

                }

                @Nested
                class GivenACustomIdResolverThatCannotResolveTheBucket {
                    @Nested
                    class GivenTheDefaultBucketIsNotSet {
                        @Nested
                        class WhenCalledWithAnEntity {
                            @BeforeEach
                            void setUp() {
                                resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                loader = mock(ResourceLoader.class);
                                placementService = mock(PlacementService.class);
                                client = mock(S3Client.class);
                                defaultBucket = null;

                                context.registerBean("s3Client", S3Client.class, new Supplier() {

                                               @Override
                                               public Object get() {
                                                   return client;
                                               }
                                }, new BeanDefinitionCustomizer[]{});
                                context = new GenericApplicationContext();
                                context.refresh();

                                defaultBucket = null;

                                entity = new TestEntity("12345-67890");

                                placementService = new PlacementServiceImpl();
                                S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
                                placementService.addConverter(new Converter<CustomContentId, S3ObjectId>() {
                                    @Override
                                    public S3ObjectId convert(CustomContentId entity) {
                                        return new S3ObjectId(null, entity.getObjectId());
                                    }
                                });

                                s3StoreImpl = new DefaultS3StoreImpl<ContentProperty, String>(context,loader,null,placementService,client,null);

                                try {
                                	r = s3StoreImpl.getResource(entity);
                                }
                                catch (Exception e) {
                                	DefaultS3StoreImplTest.this.e = e;
                                }
                            }

                            @Test
                            void shouldThrowAnException() {
                                assertThat(e).isInstanceOf(ConversionFailedException.class);
                            }

                        }

                    }

                }

            }

            @Nested
            class GetResourceWithPropertyPath {
                @Nested
                class GivenTheDefaultAssociativeStoreIdResolver {
                    @Nested
                    class GivenADefaultBucket {
                        @Nested
                        class WhenCalledWithAnEntityThatDoesnTHaveAnBucketValue {
                            @BeforeEach
                            void setUp() {
                                resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                loader = mock(ResourceLoader.class);
                                placementService = mock(PlacementService.class);
                                client = mock(S3Client.class);
                                defaultBucket = null;

                                context.registerBean("s3Client", S3Client.class, new Supplier() {

                                               @Override
                                               public Object get() {
                                                   return client;
                                               }
                                }, new BeanDefinitionCustomizer[]{});
                                context = new GenericApplicationContext();
                                context.refresh();

                                defaultBucket = "default-defaultBucket";

                                entity = new TestEntity("12345-67890");

                                placementService = new PlacementServiceImpl();
                                S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
                                placementService.addConverter(new Converter<S3ObjectId, String>() {
                                	@Override
                                	public String convert(S3ObjectId source) {
                                		return "/" + source.getKey().replaceAll("-", "/");
                                	}
                                });

                                SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver(client);
                                s3Protocol.afterPropertiesSet();
                                loader = new DefaultResourceLoader();
                                ((DefaultResourceLoader)loader).addProtocolResolver(s3Protocol);

                                s3StoreImpl = new DefaultS3StoreImpl<ContentProperty, String>(context,loader,null,placementService,client,null);

                                try {
                                	r = s3StoreImpl.getResource(entity, PropertyPath.from("content"));
                                }
                                catch (Exception e) {
                                	DefaultS3StoreImplTest.this.e = e;
                                }
                            }

                            @Test
                            void shouldFetchTheResource() {
                                assertThat(e).isNull();
                                assertThat(r).isInstanceOf(S3StoreResource.class);
                                assertThat(((S3StoreResource)r).getClient()).isEqualTo(client);
                                assertThat(r.getDescription()).isEqualTo(format("Amazon s3 resource [bucket='%s' and object='%s']","default-defaultBucket", "12345/67890"));
                            }

                        }

                        @Nested
                        class WhenCalledWithAnEntityThatHasAnBucketValue {
                            @BeforeEach
                            void setUp() {
                                resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                loader = mock(ResourceLoader.class);
                                placementService = mock(PlacementService.class);
                                client = mock(S3Client.class);
                                defaultBucket = null;

                                context.registerBean("s3Client", S3Client.class, new Supplier() {

                                               @Override
                                               public Object get() {
                                                   return client;
                                               }
                                }, new BeanDefinitionCustomizer[]{});
                                context = new GenericApplicationContext();
                                context.refresh();

                                defaultBucket = "default-defaultBucket";

                                entity = new TestEntityWithBucketAnnotation(
                                		"some-other-bucket");
                                entity.setContentId("12345-67890");

                                placementService = new PlacementServiceImpl();
                                S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

                                SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver(client);
                                s3Protocol.afterPropertiesSet();
                                loader = new DefaultResourceLoader();
                                ((DefaultResourceLoader)loader).addProtocolResolver(s3Protocol);

                                s3StoreImpl = new DefaultS3StoreImpl<ContentProperty, String>(context,loader,null,placementService,client,null);

                                try {
                                	r = s3StoreImpl.getResource(entity, PropertyPath.from("content"));
                                }
                                catch (Exception e) {
                                	DefaultS3StoreImplTest.this.e = e;
                                }
                            }

                            @Test
                            void shouldFetchTheCorrectResource() {
                                assertThat(e).isNull();
                                assertThat(r).isInstanceOf(S3StoreResource.class);
                                assertThat(((S3StoreResource)r).getClient()).isEqualTo(client);
                                assertThat(r.getDescription()).isEqualTo(format("Amazon s3 resource [bucket='%s' and object='%s']","some-other-bucket", "12345-67890"));
                            }

                        }

                        @Nested
                        class WhenCalledWithAnEntityThatHasNoAssociatedResource {
                            @BeforeEach
                            void setUp() {
                                resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                loader = mock(ResourceLoader.class);
                                placementService = mock(PlacementService.class);
                                client = mock(S3Client.class);
                                defaultBucket = null;

                                context.registerBean("s3Client", S3Client.class, new Supplier() {

                                               @Override
                                               public Object get() {
                                                   return client;
                                               }
                                }, new BeanDefinitionCustomizer[]{});
                                context = new GenericApplicationContext();
                                context.refresh();

                                defaultBucket = "default-defaultBucket";

                                entity = new TestEntity();

                                s3StoreImpl = new DefaultS3StoreImpl<ContentProperty, String>(context,loader,null,placementService,client,null);

                                try {
                                	r = s3StoreImpl.getResource(entity, PropertyPath.from("content"));
                                }
                                catch (Exception e) {
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
                class GivenACustomIdResolver {
                    @Nested
                    class GivenADefaultBucket {
                        @Nested
                        class WhenCalledWithAnEntity {
                            @BeforeEach
                            void setUp() {
                                resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                loader = mock(ResourceLoader.class);
                                placementService = mock(PlacementService.class);
                                client = mock(S3Client.class);
                                defaultBucket = null;

                                context.registerBean("s3Client", S3Client.class, new Supplier() {

                                               @Override
                                               public Object get() {
                                                   return client;
                                               }
                                }, new BeanDefinitionCustomizer[]{});
                                context = new GenericApplicationContext();
                                context.refresh();

                                defaultBucket = "default-defaultBucket";

                                entity = new TestEntity("12345-67890");

                                placementService = new PlacementServiceImpl();
                                S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

                                // Converter that matches Entity and content Id types. Expected to be invoked.
                                // Converter<ContentPropertyInfo<TestEntity, String>, S3ObjectId> instead of Converter<TestEntity, S3ObjectId> for #getResource
                                placementService.addConverter(new Converter<ContentPropertyInfo<TestEntity, String>, S3ObjectId>() {
                                	@Override
                                	public S3ObjectId convert(ContentPropertyInfo<TestEntity, String> source) {
                                		return new S3ObjectId( "custom-bucket", "test-entity/custom-object-id-string-based");
                                	}
                                });

                                // Converter that does not match content Id type. Should not be invoked.
                                placementService.addConverter(new Converter<ContentPropertyInfo<Object, UUID>, S3ObjectId>() {
                                	@Override
                                	public S3ObjectId convert(ContentPropertyInfo<Object, UUID> source) {
                                		return new S3ObjectId( "custom-bucket", "object/custom-object-id-uuid-based");
                                	}
                                });
                                // Converter that does not match Entity type. Should not be invoked.
                                placementService.addConverter(new Converter<ContentPropertyInfo<TestEntityWithBucketAnnotation, String>, S3ObjectId>() {
                                	@Override
                                	public S3ObjectId convert(ContentPropertyInfo<TestEntityWithBucketAnnotation, String> source) {
                                		return new S3ObjectId( "custom-bucket", "test-entity-with-bucket/custom-object-id-string-based");
                                	}
                                });

                                SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver(client);
                                s3Protocol.afterPropertiesSet();
                                loader = new DefaultResourceLoader();
                                ((DefaultResourceLoader)loader).addProtocolResolver(s3Protocol);

                                s3StoreImpl = new DefaultS3StoreImpl<ContentProperty, String>(context,loader,null,placementService,client,null);

                                try {
                                	r = s3StoreImpl.getResource(entity, PropertyPath.from("content"));
                                }
                                catch (Exception e) {
                                	DefaultS3StoreImplTest.this.e = e;
                                }
                            }

                            @Test
                            void shouldFetchTheResource() {
                                assertThat(e).isNull();
                                assertThat(r).isInstanceOf(S3StoreResource.class);
                                assertThat(((S3StoreResource)r).getClient()).isEqualTo(client);
                                assertThat(r.getDescription()).isEqualTo(format("Amazon s3 resource [bucket='%s' and object='%s']","custom-bucket", "test-entity/custom-object-id-string-based"));
                            }

                        }

                    }

                }

                @Nested
                class GivenACustomIdResolverThatCannotResolveTheBucket {
                    @Nested
                    class GivenTheDefaultBucketIsNotSet {
                        @Nested
                        class WhenCalledWithAnEntity {
                            @BeforeEach
                            void setUp() {
                                resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                loader = mock(ResourceLoader.class);
                                placementService = mock(PlacementService.class);
                                client = mock(S3Client.class);
                                defaultBucket = null;

                                context.registerBean("s3Client", S3Client.class, new Supplier() {

                                               @Override
                                               public Object get() {
                                                   return client;
                                               }
                                }, new BeanDefinitionCustomizer[]{});
                                context = new GenericApplicationContext();
                                context.refresh();

                                defaultBucket = null;

                                entity = new TestEntity("12345-67890");

                                placementService = new PlacementServiceImpl();
                                S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
                                placementService.addConverter(new Converter<ContentPropertyInfo<TestEntity, String>, S3ObjectId>() {
                                	@Override
                                	public S3ObjectId convert(ContentPropertyInfo<TestEntity, String> source) {
                                		return new S3ObjectId(null, "custom-object-id");
                                	}
                                });

                                s3StoreImpl = new DefaultS3StoreImpl<ContentProperty, String>(context,loader,null,placementService,client,null);

                                try {
                                	r = s3StoreImpl.getResource(entity, PropertyPath.from("content"));
                                }
                                catch (Exception e) {
                                	DefaultS3StoreImplTest.this.e = e;
                                }
                            }

                            @Test
                            void shouldThrowAnException() {
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

                    context.registerBean("s3Client", S3Client.class, new Supplier() {

                                   @Override
                                   public Object get() {
                                       return client;
                                   }
                    }, new BeanDefinitionCustomizer[]{});
                    context = new GenericApplicationContext();
                    context.refresh();

                    id = "12345-67890";
                    entity = new TestEntity();

                    s3StoreImpl = new DefaultS3StoreImpl<ContentProperty, String>(context,loader,null,placementService,client,null);

                    s3StoreImpl.associate(entity, id);
                }

                @Test
                void shouldSetTheEntitySContentIDAttribute() {
                    assertThat(entity.getContentId()).isEqualTo("12345-67890");
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

                    context.registerBean("s3Client", S3Client.class, new Supplier() {

                                   @Override
                                   public Object get() {
                                       return client;
                                   }
                    }, new BeanDefinitionCustomizer[]{});
                    context = new GenericApplicationContext();
                    context.refresh();

                    entity = new TestEntity();
                    entity.setContentId("12345-67890");

                    s3StoreImpl = new DefaultS3StoreImpl<ContentProperty, String>(context,loader,null,placementService,client,null);

                    s3StoreImpl.unassociate(entity);
                }

                @Test
                void shouldResetTheEntitySContentIDAttribute() {
                    assertThat(entity.getContentId()).isNull();
                }

            }

        }

        @Nested
        class ContentStore {
            @Nested
            class SetContent {
                @Nested
                class GivenTheDefaultAssociativeStoreIdResolver {
                    @Nested
                    class GivenADefaultBucketIsSet {
                        @Nested
                        class WhenTheContentAlreadyExists {
                            @BeforeEach
                            void setUp() throws IOException {
                                resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                loader = mock(ResourceLoader.class);
                                placementService = mock(PlacementService.class);
                                client = mock(S3Client.class);
                                defaultBucket = null;

                                context.registerBean("s3Client", S3Client.class, new Supplier() {

                                               @Override
                                               public Object get() {
                                                   return client;
                                               }
                                }, new BeanDefinitionCustomizer[]{});
                                context = new GenericApplicationContext();
                                context.refresh();

                                entity = new TestEntity();
                                content = new ByteArrayInputStream(
                                		"Hello content world!".getBytes());

                                defaultBucket = "default-defaultBucket";

                                entity.setContentId("abcd-efgh");

                                                          placementService = new PlacementServiceImpl();
                                                          S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

                                when(loader.getResource(endsWith("abcd-efgh"))).thenReturn(resource);
                                output = mock(OutputStream.class);
                                when(resource.getOutputStream()).thenReturn(output);

                                when(resource.contentLength()).thenReturn(20L);

                                when(resource.exists()).thenReturn(true);

                                s3StoreImpl = spy(new DefaultS3StoreImpl<ContentProperty, String>(context,loader,null,placementService,client,null));

                                try {
                                	s3StoreImpl.setContent(entity, content);
                                } catch (Exception e) {
                                	DefaultS3StoreImplTest.this.e = e;
                                }
                            }

                            @Test
                            void shouldFetchTheResource() {
                                verify(loader).getResource(eq("s3://default-defaultBucket/abcd-efgh"));
                            }

                            @Test
                            void shouldChangeTheContentLength() {
                                assertThat(entity.getContentLen()).isEqualTo(20L);
                            }

                            @Test
                            void shouldWriteToTheResourceSOutputstream() throws IOException {
                                verify(resource).getOutputStream();
                                verify(output, times(1)).write(any(byte[].class),
                                		eq(0), eq(20));
                            }

                            @Nested
                            class WhenTheResourceOutputStreamThrowsAnIOException {
                                @BeforeEach
                                void setUp() throws IOException {
                                    resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                    loader = mock(ResourceLoader.class);
                                    placementService = mock(PlacementService.class);
                                    client = mock(S3Client.class);
                                    defaultBucket = null;

                                    context.registerBean("s3Client", S3Client.class, new Supplier() {

                                                   @Override
                                                   public Object get() {
                                                       return client;
                                                   }
                                    }, new BeanDefinitionCustomizer[]{});
                                    context = new GenericApplicationContext();
                                    context.refresh();

                                    entity = new TestEntity();
                                    content = new ByteArrayInputStream(
                                    		"Hello content world!".getBytes());

                                    defaultBucket = "default-defaultBucket";

                                    entity.setContentId("abcd-efgh");

                                                              placementService = new PlacementServiceImpl();
                                                              S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

                                    when(loader.getResource(endsWith("abcd-efgh"))).thenReturn(resource);
                                    output = mock(OutputStream.class);
                                    when(resource.getOutputStream()).thenReturn(output);

                                    when(resource.contentLength()).thenReturn(20L);

                                    when(resource.exists()).thenReturn(true);

                                    when(resource.getOutputStream()).thenThrow(new IOException("set-ioexception"));

                                    s3StoreImpl = spy(new DefaultS3StoreImpl<ContentProperty, String>(context,loader,null,placementService,client,null));

                                    try {
                                    	s3StoreImpl.setContent(entity, content);
                                    } catch (Exception e) {
                                    	DefaultS3StoreImplTest.this.e = e;
                                    }
                                }

                                @Test
                                void shouldThrowAStoreAccessException() {
                                    assertThat(e).isInstanceOf(StoreAccessException.class);
                                    assertThat(e.getCause().getMessage()).isEqualTo("set-ioexception");
                                }

                            }

                        }

                        @Nested
                        class WhenTheContentDoesNotAlreadyExist {
                            @BeforeEach
                            void setUp() throws IOException {
                                resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                loader = mock(ResourceLoader.class);
                                placementService = mock(PlacementService.class);
                                client = mock(S3Client.class);
                                defaultBucket = null;

                                context.registerBean("s3Client", S3Client.class, new Supplier() {

                                               @Override
                                               public Object get() {
                                                   return client;
                                               }
                                }, new BeanDefinitionCustomizer[]{});
                                context = new GenericApplicationContext();
                                context.refresh();

                                entity = new TestEntity();
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
                                parent = mock(File.class);

                                when(resource.getFile()).thenReturn(resourceFile);
                                when(resourceFile.getParentFile()).thenReturn(parent);

                                s3StoreImpl = spy(new DefaultS3StoreImpl<ContentProperty, String>(context,loader,null,placementService,client,null));

                                try {
                                	s3StoreImpl.setContent(entity, content);
                                } catch (Exception e) {
                                	DefaultS3StoreImplTest.this.e = e;
                                }
                            }

                            @Test
                            void shouldMakeANewUUID() {
                                assertThat(entity.getContentId()).isNotNull();
                            }

                            @Test
                            void shouldCreateANewResource() {
                                verify(loader).getResource(matches("^s3://.*[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"));
                            }

                            @Test
                            void shouldWriteToTheResourceSOutputstream() throws IOException {
                                verify(resource).getOutputStream();
                                verify(output, times(1)).write(any(byte[].class),
                                		eq(0), eq(20));
                            }

                        }

                        @Nested
                        class WhenS3ThrowsAnS3Exception {
                            @BeforeEach
                            void setUp() throws IOException {
                                resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                loader = mock(ResourceLoader.class);
                                placementService = mock(PlacementService.class);
                                client = mock(S3Client.class);
                                defaultBucket = null;

                                context.registerBean("s3Client", S3Client.class, new Supplier() {

                                               @Override
                                               public Object get() {
                                                   return client;
                                               }
                                }, new BeanDefinitionCustomizer[]{});
                                context = new GenericApplicationContext();
                                context.refresh();

                                entity = new TestEntity();
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

                                s3StoreImpl = spy(new DefaultS3StoreImpl<ContentProperty, String>(context,loader,null,placementService,client,null));

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

                    context.registerBean("s3Client", S3Client.class, new Supplier() {

                                   @Override
                                   public Object get() {
                                       return client;
                                   }
                    }, new BeanDefinitionCustomizer[]{});
                    context = new GenericApplicationContext();
                    context.refresh();

                    entity = new TestEntity();
                    content = new ByteArrayInputStream("Hello content world!".getBytes());
                    r = new InputStreamResource(content);

                    s3StoreImpl = spy(new DefaultS3StoreImpl<ContentProperty, String>(context,loader,null,placementService,client,null));

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
                class WhenTheResourceThrowsAnIOException {
                    @BeforeEach
                    void setUp() throws IOException {
                        resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                        loader = mock(ResourceLoader.class);
                        placementService = mock(PlacementService.class);
                        client = mock(S3Client.class);
                        defaultBucket = null;

                        context.registerBean("s3Client", S3Client.class, new Supplier() {

                                       @Override
                                       public Object get() {
                                           return client;
                                       }
                        }, new BeanDefinitionCustomizer[]{});
                        context = new GenericApplicationContext();
                        context.refresh();

                        entity = new TestEntity();
                        content = new ByteArrayInputStream("Hello content world!".getBytes());
                        r = new InputStreamResource(content);

                        r = mock(Resource.class);
                        when(r.getInputStream()).thenThrow(new IOException("setContent badness"));

                        s3StoreImpl = spy(new DefaultS3StoreImpl<ContentProperty, String>(context,loader,null,placementService,client,null));

                        try {
                        	s3StoreImpl.setContent(entity, r);
                        } catch (Exception e) {
                        	DefaultS3StoreImplTest.this.e = e;
                        }
                    }

                    @Test
                    void shouldThrowAStoreAccessException() {
                        assertThat(e).isInstanceOf(StoreAccessException.class);
                        assertThat(e.getCause().getMessage()).contains("setContent badness");
                    }

                }

            }

            @Nested
            class GetContent {
                @Nested
                class GivenTheDefaultAssociativeStoreIdResolver {
                    @Nested
                    class GivenADefaultBucketIsSet {
                        @Nested
                        class WhenCalledWithAnEntity {
                            @Nested
                            class AndTheResourceAlreadyExists {
                                @BeforeEach
                                void setUp() throws IOException {
                                    resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                    loader = mock(ResourceLoader.class);
                                    placementService = mock(PlacementService.class);
                                    client = mock(S3Client.class);
                                    defaultBucket = null;

                                    context.registerBean("s3Client", S3Client.class, new Supplier() {

                                                   @Override
                                                   public Object get() {
                                                       return client;
                                                   }
                                    }, new BeanDefinitionCustomizer[]{});
                                    context = new GenericApplicationContext();
                                    context.refresh();

                                    defaultBucket = "default-defaultBucket";

                                    entity = new TestEntity();
                                    content = mock(InputStream.class);
                                    entity.setContentId("abcd-efgh");

                                                              placementService = new PlacementServiceImpl();
                                                              S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

                                                              when(loader.getResource(matches("^s3://default-defaultBucket/abcd-efgh"))).thenReturn(resource);
                                    when(resource.getInputStream()).thenReturn(content);

                                    when(resource.exists()).thenReturn(true);

                                    s3StoreImpl = spy(new DefaultS3StoreImpl<ContentProperty, String>(context,loader,null,placementService,client,null));

                                    try {
                                    	result = s3StoreImpl.getContent(entity);
                                    } catch (Exception e) {
                                    	DefaultS3StoreImplTest.this.e = e;
                                    }
                                }

                                @Test
                                void shouldFetchTheResource() {
                                    verify(loader).getResource(eq("s3://default-defaultBucket/abcd-efgh"));
                                }

                                @Test
                                void shouldGetContent() {
                                    assertThat(result).isEqualTo(content);
                                }

                                @Nested
                                class WhenTheResourceInputStreamThrowsAnIOException {
                                    @BeforeEach
                                    void setUp() throws IOException {
                                        resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                        loader = mock(ResourceLoader.class);
                                        placementService = mock(PlacementService.class);
                                        client = mock(S3Client.class);
                                        defaultBucket = null;

                                        context.registerBean("s3Client", S3Client.class, new Supplier() {

                                                       @Override
                                                       public Object get() {
                                                           return client;
                                                       }
                                        }, new BeanDefinitionCustomizer[]{});
                                        context = new GenericApplicationContext();
                                        context.refresh();

                                        defaultBucket = "default-defaultBucket";

                                        entity = new TestEntity();
                                        content = mock(InputStream.class);
                                        entity.setContentId("abcd-efgh");

                                                                  placementService = new PlacementServiceImpl();
                                                                  S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

                                                                  when(loader.getResource(matches("^s3://default-defaultBucket/abcd-efgh"))).thenReturn(resource);
                                        when(resource.getInputStream()).thenReturn(content);

                                        when(resource.exists()).thenReturn(true);

                                        when(resource.getInputStream()).thenThrow(new IOException("get-ioexception"));

                                        s3StoreImpl = spy(new DefaultS3StoreImpl<ContentProperty, String>(context,loader,null,placementService,client,null));

                                        try {
                                        	result = s3StoreImpl.getContent(entity);
                                        } catch (Exception e) {
                                        	DefaultS3StoreImplTest.this.e = e;
                                        }
                                    }

                                    @Test
                                    void shouldThrowAStoreAccessException() {
                                        assertThat(e).isInstanceOf(StoreAccessException.class);
                                        assertThat(e.getCause().getMessage()).isEqualTo("get-ioexception");
                                    }

                                }

                            }

                            @Nested
                            class AndTheResourceDoesnTExist {
                                @BeforeEach
                                void setUp() throws IOException {
                                    resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                    loader = mock(ResourceLoader.class);
                                    placementService = mock(PlacementService.class);
                                    client = mock(S3Client.class);
                                    defaultBucket = null;

                                    context.registerBean("s3Client", S3Client.class, new Supplier() {

                                                   @Override
                                                   public Object get() {
                                                       return client;
                                                   }
                                    }, new BeanDefinitionCustomizer[]{});
                                    context = new GenericApplicationContext();
                                    context.refresh();

                                    defaultBucket = "default-defaultBucket";

                                    entity = new TestEntity();
                                    content = mock(InputStream.class);
                                    entity.setContentId("abcd-efgh");

                                                              placementService = new PlacementServiceImpl();
                                                              S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

                                                              when(loader.getResource(matches("^s3://default-defaultBucket/abcd-efgh"))).thenReturn(resource);
                                    when(resource.getInputStream()).thenReturn(content);

                                    nonExistentResource = mock(WritableResource.class);
                                    when(resource.exists()).thenReturn(true);

                                    when(loader.getResource(endsWith("abcd-efgh"))).thenReturn(nonExistentResource);

                                    s3StoreImpl = spy(new DefaultS3StoreImpl<ContentProperty, String>(context,loader,null,placementService,client,null));

                                    try {
                                    	result = s3StoreImpl.getContent(entity);
                                    } catch (Exception e) {
                                    	DefaultS3StoreImplTest.this.e = e;
                                    }
                                }

                                @Test
                                void shouldFetchTheResource() {
                                    verify(loader).getResource(eq("s3://default-defaultBucket/abcd-efgh"));
                                }

                                @Test
                                void shouldNotFindTheContent() {
                                    assertThat(result).isNull();
                                }

                            }

                            @Nested
                            class WithAnNullContentId {
                                @BeforeEach
                                void setUp() throws IOException {
                                    resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                    loader = mock(ResourceLoader.class);
                                    placementService = mock(PlacementService.class);
                                    client = mock(S3Client.class);
                                    defaultBucket = null;

                                    context.registerBean("s3Client", S3Client.class, new Supplier() {

                                                   @Override
                                                   public Object get() {
                                                       return client;
                                                   }
                                    }, new BeanDefinitionCustomizer[]{});
                                    context = new GenericApplicationContext();
                                    context.refresh();

                                    defaultBucket = "default-defaultBucket";

                                    entity = new TestEntity();
                                    content = mock(InputStream.class);
                                    entity.setContentId("abcd-efgh");

                                                              placementService = new PlacementServiceImpl();
                                                              S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

                                                              when(loader.getResource(matches("^s3://default-defaultBucket/abcd-efgh"))).thenReturn(resource);
                                    when(resource.getInputStream()).thenReturn(content);

                                    entity.setContentId(null);

                                    s3StoreImpl = spy(new DefaultS3StoreImpl<ContentProperty, String>(context,loader,null,placementService,client,null));

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
                class GivenTheDefaultAssociativeStoreIdResolver {
                    @Nested
                    class GivenADefaultBucketIsSet {
                        @Nested
                        class WhenCalledWithAnEntity {
                            @Nested
                            class AndTheContentExists {
                                @BeforeEach
                                void setUp() {
                                    resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                    loader = mock(ResourceLoader.class);
                                    placementService = mock(PlacementService.class);
                                    client = mock(S3Client.class);
                                    defaultBucket = null;

                                    context.registerBean("s3Client", S3Client.class, new Supplier() {

                                                   @Override
                                                   public Object get() {
                                                       return client;
                                                   }
                                    }, new BeanDefinitionCustomizer[]{});
                                    context = new GenericApplicationContext();
                                    context.refresh();

                                    defaultBucket = "default-defaultBucket";

                                    entity = new TestEntity();
                                    entity.setContentId("abcd-efgh");
                                    entity.setContentLen(100L);
                                    resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));

                                    placementService = new PlacementServiceImpl();
                                    S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

                                    when(loader.getResource(endsWith("abcd-efgh"))).thenReturn(resource);
                                    when(resource.exists()).thenReturn(true);

                                    s3StoreImpl = spy(new DefaultS3StoreImpl<ContentProperty, String>(context,loader,null,placementService,client,null));

                                    try {
                                    	s3StoreImpl.unsetContent(entity);
                                    } catch (Exception e) {
                                    	DefaultS3StoreImplTest.this.e = e;
                                    }
                                }

                                @Test
                                void shouldFetchTheResource() {
                                    verify(loader).getResource(eq("s3://default-defaultBucket/abcd-efgh"));
                                }

                                @Nested
                                class WhenThePropertyHasADedicatedContentIdField {
                                    @BeforeEach
                                    void setUp() {
                                        resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                        loader = mock(ResourceLoader.class);
                                        placementService = mock(PlacementService.class);
                                        client = mock(S3Client.class);
                                        defaultBucket = null;

                                        context.registerBean("s3Client", S3Client.class, new Supplier() {

                                                       @Override
                                                       public Object get() {
                                                           return client;
                                                       }
                                        }, new BeanDefinitionCustomizer[]{});
                                        context = new GenericApplicationContext();
                                        context.refresh();

                                        defaultBucket = "default-defaultBucket";

                                        entity = new TestEntity();
                                        entity.setContentId("abcd-efgh");
                                        entity.setContentLen(100L);
                                        resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));

                                        placementService = new PlacementServiceImpl();
                                        S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

                                        when(loader.getResource(endsWith("abcd-efgh"))).thenReturn(resource);
                                        when(resource.exists()).thenReturn(true);

                                        s3StoreImpl = spy(new DefaultS3StoreImpl<ContentProperty, String>(context,loader,null,placementService,client,null));

                                        try {
                                        	s3StoreImpl.unsetContent(entity);
                                        } catch (Exception e) {
                                        	DefaultS3StoreImplTest.this.e = e;
                                        }
                                    }

                                    @Test
                                    void shouldResetTheMetadata() {
                                        assertThat(entity.getContentId()).isNull();
                                        assertThat(entity.getContentLen()).isEqualTo(0L);
                                    }

                                }

                                @Nested
                                class WhenThePropertySContentIdFieldAlsoIsTheJakartaPersistenceIdField {
                                    @BeforeEach
                                    void setUp() {
                                        resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                        loader = mock(ResourceLoader.class);
                                        placementService = mock(PlacementService.class);
                                        client = mock(S3Client.class);
                                        defaultBucket = null;

                                        context.registerBean("s3Client", S3Client.class, new Supplier() {

                                                       @Override
                                                       public Object get() {
                                                           return client;
                                                       }
                                        }, new BeanDefinitionCustomizer[]{});
                                        context = new GenericApplicationContext();
                                        context.refresh();

                                        defaultBucket = "default-defaultBucket";

                                        entity = new TestEntity();
                                        entity.setContentId("abcd-efgh");
                                        entity.setContentLen(100L);
                                        resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));

                                        placementService = new PlacementServiceImpl();
                                        S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

                                        when(loader.getResource(endsWith("abcd-efgh"))).thenReturn(resource);
                                        when(resource.exists()).thenReturn(true);

                                        entity = new SharedIdContentIdEntity();
                                        entity.setContentId("abcd-efgh");

                                        s3StoreImpl = spy(new DefaultS3StoreImpl<ContentProperty, String>(context,loader,null,placementService,client,null));

                                        try {
                                        	s3StoreImpl.unsetContent(entity);
                                        } catch (Exception e) {
                                        	DefaultS3StoreImplTest.this.e = e;
                                        }
                                    }

                                    @Test
                                    void shouldNotResetTheContentIdMetadata() {
                                        assertThat(entity.getContentId()).isEqualTo("abcd-efgh");
                                        assertThat(entity.getContentLen()).isEqualTo(0L);
                                    }

                                }

                                @Nested
                                class WhenThePropertySContentIdFieldAlsoIsTheSpringIdField {
                                    @BeforeEach
                                    void setUp() {
                                        resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                        loader = mock(ResourceLoader.class);
                                        placementService = mock(PlacementService.class);
                                        client = mock(S3Client.class);
                                        defaultBucket = null;

                                        context.registerBean("s3Client", S3Client.class, new Supplier() {

                                                       @Override
                                                       public Object get() {
                                                           return client;
                                                       }
                                        }, new BeanDefinitionCustomizer[]{});
                                        context = new GenericApplicationContext();
                                        context.refresh();

                                        defaultBucket = "default-defaultBucket";

                                        entity = new TestEntity();
                                        entity.setContentId("abcd-efgh");
                                        entity.setContentLen(100L);
                                        resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));

                                        placementService = new PlacementServiceImpl();
                                        S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

                                        when(loader.getResource(endsWith("abcd-efgh"))).thenReturn(resource);
                                        when(resource.exists()).thenReturn(true);

                                        entity = new SharedSpringIdContentIdEntity();
                                        entity.setContentId("abcd-efgh");

                                        s3StoreImpl = spy(new DefaultS3StoreImpl<ContentProperty, String>(context,loader,null,placementService,client,null));

                                        try {
                                        	s3StoreImpl.unsetContent(entity);
                                        } catch (Exception e) {
                                        	DefaultS3StoreImplTest.this.e = e;
                                        }
                                    }

                                    @Test
                                    void shouldNotResetTheContentIdMetadata() {
                                        assertThat(entity.getContentId()).isEqualTo("abcd-efgh");
                                        assertThat(entity.getContentLen()).isEqualTo(0L);
                                    }

                                }

                            }

                            @Nested
                            class AndTheContentDoesnTExist {
                                @BeforeEach
                                void setUp() {
                                    resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                    loader = mock(ResourceLoader.class);
                                    placementService = mock(PlacementService.class);
                                    client = mock(S3Client.class);
                                    defaultBucket = null;

                                    context.registerBean("s3Client", S3Client.class, new Supplier() {

                                                   @Override
                                                   public Object get() {
                                                       return client;
                                                   }
                                    }, new BeanDefinitionCustomizer[]{});
                                    context = new GenericApplicationContext();
                                    context.refresh();

                                    defaultBucket = "default-defaultBucket";

                                    entity = new TestEntity();
                                    entity.setContentId("abcd-efgh");
                                    entity.setContentLen(100L);
                                    resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));

                                    placementService = new PlacementServiceImpl();
                                    S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

                                    nonExistentResource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
                                    when(loader.getResource(endsWith("abcd-efgh"))).thenReturn(nonExistentResource);
                                    when(nonExistentResource.exists()).thenReturn(false);

                                    s3StoreImpl = spy(new DefaultS3StoreImpl<ContentProperty, String>(context,loader,null,placementService,client,null));

                                    try {
                                    	s3StoreImpl.unsetContent(entity);
                                    } catch (Exception e) {
                                    	DefaultS3StoreImplTest.this.e = e;
                                    }
                                }

                                @Test
                                void shouldFetchTheResource() {
                                    verify(loader).getResource(eq("s3://default-defaultBucket/abcd-efgh"));
                                }

                                @Test
                                void shouldUnsetTheContent() {
                                    verify(client, never()).deleteObject(any(DeleteObjectRequest.class));
                                    assertThat(entity.getContentId()).isNull();
                                    assertThat(entity.getContentLen()).isEqualTo(0L);
                                }

                            }

                        }

                    }

                }

            }

        }

    }

	public interface ContentProperty {
		String getContentId();

		void setContentId(String contentId);

		long getContentLen();

		void setContentLen(long contentLen);
	}

	public static class TestEntity implements ContentProperty {
		@ContentId
		private String contentId;

		@ContentLength
		private long contentLen;

		public TestEntity() {
			this.contentId = null;
		}

		public TestEntity(String contentId) {
			this.contentId = new String(contentId);
		}

		@Override
       public String getContentId() {
			return this.contentId;
		}

		@Override
       public void setContentId(String contentId) {
			this.contentId = contentId;
		}

		@Override
       public long getContentLen() {
			return contentLen;
		}

		@Override
       public void setContentLen(long contentLen) {
			this.contentLen = contentLen;
		}
	}

	public class TestEntityWithBucketAnnotation extends TestEntity {
		@Bucket
		private String bucketId = null;

		public TestEntityWithBucketAnnotation(String bucketId) {
			this.bucketId = bucketId;
		}

		public String getBucketId() {
			return bucketId;
		}

		public void setBucketId(String bucketId) {
			this.bucketId = bucketId;
		}
	}

	public static class SharedIdContentIdEntity implements ContentProperty {

		@jakarta.persistence.Id
		@ContentId
		private String contentId;

		@ContentLength
		private long contentLen;

		public SharedIdContentIdEntity() {
			this.contentId = null;
		}

		@Override
       public String getContentId() {
			return this.contentId;
		}

		@Override
       public void setContentId(String contentId) {
			this.contentId = contentId;
		}

		@Override
       public long getContentLen() {
			return contentLen;
		}

		@Override
       public void setContentLen(long contentLen) {
			this.contentLen = contentLen;
		}
	}

	public static class SharedSpringIdContentIdEntity implements ContentProperty {

		@org.springframework.data.annotation.Id
		@ContentId
		private String contentId;

		@ContentLength
		private long contentLen;

		public SharedSpringIdContentIdEntity() {
			this.contentId = null;
		}

		@Override
       public String getContentId() {
			return this.contentId;
		}

		@Override
       public void setContentId(String contentId) {
			this.contentId = contentId;
		}

		@Override
       public long getContentLen() {
			return contentLen;
		}

		@Override
       public void setContentLen(long contentLen) {
			this.contentLen = contentLen;
		}
	}

	public class CustomContentId implements Serializable {
		private String customer;
		private String objectId;

		public CustomContentId(String bucket, String objectId) {
			this.customer = bucket;
			this.objectId = objectId;
		}

		public String getCustomer() {
			return customer;
		}

		public void setCustomer(String customer) {
			this.customer = customer;
		}

		public String getObjectId() {
			return objectId;
		}

		public void setObjectId(String objectId) {
			this.objectId = objectId;
		}
	}
}
