package internal.org.springframework.content.mongo.it;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.assertj.core.api.Assertions.assertThat;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.*;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.content.mongo.config.EnableMongoStores;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.mongodb.client.MongoClient;
import com.mongodb.client.gridfs.model.GridFSFile;

import internal.org.springframework.content.mongo.store.DefaultMongoStoreImpl;
import net.bytebuddy.utility.RandomString;

public class MongoStoreIT {
	private DefaultMongoStoreImpl<Object, String> mongoContentRepoImpl;
	private GridFsTemplate gridFsTemplate;
	private GridFSFile gridFSFile;
	private ObjectId gridFSId;
	private TestEntity entity;
	private GridFsResource resource;
	private Resource genericResource;
	private PlacementService placer;

	private InputStream content;
	private InputStream result;
	private Exception e;

	private AnnotationConfigApplicationContext context;

	private TestEntityRepository repo;
	private TestEntityStore store;

	private String resourceLocation;


	
    @Nested
    class DefaultMongoStoreImplCases {
        @Nested
        class StoreCases {
            @Nested
            class GetResource {
                @Nested
                class Tests {
                    @BeforeEach
                    void setUp() throws Throwable {
                        context = new AnnotationConfigApplicationContext();
                        				context.register(TestConfig.class);
                        				context.refresh();

                        				gridFsTemplate = context.getBean(GridFsTemplate.class);

                        				repo = context.getBean(TestEntityRepository.class);
                        				store = context.getBean(TestEntityStore.class);

                        				RandomString random  = new RandomString(5);
                        				resourceLocation = random.nextString();

                        genericResource = store.getResource(resourceLocation);
                    }
                    @AfterEach
                    void tearDown() throws Throwable {
                        ((DeletableResource)genericResource).delete();

                        context.close();
                    }
                    @Test
                    void shouldGetResource() throws Throwable {
                        assertThat(genericResource).isInstanceOf(Resource.class);
                    }
                    @Test
                    void shouldNotExist() throws Throwable {
                        assertThat(genericResource.exists()).isFalse();
                    }
                }
                @Nested
                class GivenContentIsAddedToThatResource {
                    @Nested
                    class Tests {
                        @BeforeEach
                        void setUp() throws Throwable {
                            context = new AnnotationConfigApplicationContext();
                            				context.register(TestConfig.class);
                            				context.refresh();

                            				gridFsTemplate = context.getBean(GridFsTemplate.class);

                            				repo = context.getBean(TestEntityRepository.class);
                            				store = context.getBean(TestEntityStore.class);

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
                        void tearDown() throws Throwable {
                            ((DeletableResource)genericResource).delete();

                            context.close();
                        }
                        @Test
                        void shouldStoreThatContent() throws Throwable {
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
                        void setUp() throws Throwable {
                            context = new AnnotationConfigApplicationContext();
                            				context.register(TestConfig.class);
                            				context.refresh();

                            				gridFsTemplate = context.getBean(GridFsTemplate.class);

                            				repo = context.getBean(TestEntityRepository.class);
                            				store = context.getBean(TestEntityStore.class);

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
                        void tearDown() throws Throwable {
                            ((DeletableResource)genericResource).delete();

                            context.close();
                        }
                        @Test
                        void shouldStoreThatUpdatedContent() throws Throwable {
                            assertThat(genericResource.exists()).isTrue();

                            								try (InputStream expected = new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes())) {
                            									try (InputStream actual = genericResource.getInputStream()) {
                            										assertThat(IOUtils.contentEquals(expected, actual)).isTrue();
                            									}
                            								}
                        }
                    }
                    @Nested
                    class GivenThatResourceIsThenDeleted {
                        @BeforeEach
                        void setUp() throws Throwable {
                            context = new AnnotationConfigApplicationContext();
                            				context.register(TestConfig.class);
                            				context.refresh();

                            				gridFsTemplate = context.getBean(GridFsTemplate.class);

                            				repo = context.getBean(TestEntityRepository.class);
                            				store = context.getBean(TestEntityStore.class);

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
                            									MongoStoreIT.this.e = e;
                            								}
                        }
                        @AfterEach
                        void tearDown() throws Throwable {
                            ((DeletableResource)genericResource).delete();

                            context.close();
                        }
                        @Test
                        void shouldNotExist() throws Throwable {
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
                    void setUp() throws Throwable {
                        context = new AnnotationConfigApplicationContext();
                        				context.register(TestConfig.class);
                        				context.refresh();

                        				gridFsTemplate = context.getBean(GridFsTemplate.class);

                        				repo = context.getBean(TestEntityRepository.class);
                        				store = context.getBean(TestEntityStore.class);

                        				RandomString random  = new RandomString(5);
                        				resourceLocation = random.nextString();

                        entity = new TestEntity();
                                                entity = repo.save(entity);
                    }
                    @AfterEach
                    void tearDown() throws Throwable {
                        context.close();
                    }
                    @Test
                    void shouldNotHaveAnAssociatedResource() throws Throwable {
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
                            void setUp() throws Throwable {
                                context = new AnnotationConfigApplicationContext();
                                				context.register(TestConfig.class);
                                				context.refresh();

                                				gridFsTemplate = context.getBean(GridFsTemplate.class);

                                				repo = context.getBean(TestEntityRepository.class);
                                				store = context.getBean(TestEntityStore.class);

                                				RandomString random  = new RandomString(5);
                                				resourceLocation = random.nextString();

                                entity = new TestEntity();
                                                        entity = repo.save(entity);

                                genericResource = store.getResource(resourceLocation);

                                store.associate(entity, resourceLocation);
                                                                store.associate(entity, PropertyPath.from("rendition"), resourceLocation);
                            }
                            @AfterEach
                            void tearDown() throws Throwable {
                                context.close();
                            }
                            @Test
                            void shouldBeRecordedAsSuchOnTheEntitySContentId() throws Throwable {
                                assertThat(entity.getContentId()).isEqualTo(resourceLocation);
                                                                assertThat(entity.getRenditionId()).isEqualTo(resourceLocation);
                            }
                        }
                        @Nested
                        class WhenTheResourceHasContent {
                            @BeforeEach
                            void setUp() throws Throwable {
                                context = new AnnotationConfigApplicationContext();
                                				context.register(TestConfig.class);
                                				context.refresh();

                                				gridFsTemplate = context.getBean(GridFsTemplate.class);

                                				repo = context.getBean(TestEntityRepository.class);
                                				store = context.getBean(TestEntityStore.class);

                                				RandomString random  = new RandomString(5);
                                				resourceLocation = random.nextString();

                                entity = new TestEntity();
                                                        entity = repo.save(entity);

                                genericResource = store.getResource(resourceLocation);

                                store.associate(entity, resourceLocation);
                                                                store.associate(entity, PropertyPath.from("rendition"), resourceLocation);

                                try (OutputStream os = ((WritableResource)genericResource).getOutputStream()) {
                                										os.write("Hello Client-side World!".getBytes());
                                									}
                            }
                            @AfterEach
                            void tearDown() throws Throwable {
                                context.close();
                            }
                            @Test
                            void shouldNotHonorByteRanges() throws Throwable {
                                // relies on REST-layer to serve byte range
                                									Resource r = store.getResource(entity, PropertyPath.from("content"), new GetResourceParams("5-10"));
                                									try (InputStream is = r.getInputStream()) {
                                										assertThat(IOUtils.toString(is)).isEqualTo("Hello Client-side World!");
                                									}
                            }
                        }
                        @Nested
                        class WhenTheResourceIsUnassociated {
                            @BeforeEach
                            void setUp() throws Throwable {
                                context = new AnnotationConfigApplicationContext();
                                				context.register(TestConfig.class);
                                				context.refresh();

                                				gridFsTemplate = context.getBean(GridFsTemplate.class);

                                				repo = context.getBean(TestEntityRepository.class);
                                				store = context.getBean(TestEntityStore.class);

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
                            @AfterEach
                            void tearDown() throws Throwable {
                                context.close();
                            }
                            @Test
                            void shouldResetTheEntitySContentId() throws Throwable {
                                assertThat(entity.getContentId()).isNull();
                                                                    assertThat(entity.getRenditionId()).isNull();
                            }
                        }
                        @Nested
                        class WhenAInvalidPropertyPathIsUsedToAssociateAResource {
                            @BeforeEach
                            void setUp() throws Throwable {
                                context = new AnnotationConfigApplicationContext();
                                				context.register(TestConfig.class);
                                				context.refresh();

                                				gridFsTemplate = context.getBean(GridFsTemplate.class);

                                				repo = context.getBean(TestEntityRepository.class);
                                				store = context.getBean(TestEntityStore.class);

                                				RandomString random  = new RandomString(5);
                                				resourceLocation = random.nextString();

                                entity = new TestEntity();
                                                        entity = repo.save(entity);

                                genericResource = store.getResource(resourceLocation);

                                store.associate(entity, resourceLocation);
                                                                store.associate(entity, PropertyPath.from("rendition"), resourceLocation);
                            }
                            @AfterEach
                            void tearDown() throws Throwable {
                                context.close();
                            }
                            @Test
                            void shouldThrowAnError() throws Throwable {
                                try {
                                                                        store.associate(entity, PropertyPath.from("does.not.exist"), resourceLocation);
                                                                    } catch (Exception sae) {
                                                                        MongoStoreIT.this.e = sae;
                                                                    }
                                                                    assertThat(e).isInstanceOf(StoreAccessException.class);
                            }
                        }
                        @Nested
                        class WhenAInvalidPropertyPathIsUsedToLoadAResource {
                            @BeforeEach
                            void setUp() throws Throwable {
                                context = new AnnotationConfigApplicationContext();
                                				context.register(TestConfig.class);
                                				context.refresh();

                                				gridFsTemplate = context.getBean(GridFsTemplate.class);

                                				repo = context.getBean(TestEntityRepository.class);
                                				store = context.getBean(TestEntityStore.class);

                                				RandomString random  = new RandomString(5);
                                				resourceLocation = random.nextString();

                                entity = new TestEntity();
                                                        entity = repo.save(entity);

                                genericResource = store.getResource(resourceLocation);

                                store.associate(entity, resourceLocation);
                                                                store.associate(entity, PropertyPath.from("rendition"), resourceLocation);
                            }
                            @AfterEach
                            void tearDown() throws Throwable {
                                context.close();
                            }
                            @Test
                            void shouldThrowAnError() throws Throwable {
                                try {
                                                                        store.getResource(entity, PropertyPath.from("does.not.exist"));
                                                                    } catch (Exception sae) {
                                                                        MongoStoreIT.this.e = sae;
                                                                    }
                                                                    assertThat(e).isInstanceOf(StoreAccessException.class);
                            }
                        }
                        @Nested
                        class WhenAInvalidPropertyPathIsUsedToUnassociateAResource {
                            @BeforeEach
                            void setUp() throws Throwable {
                                context = new AnnotationConfigApplicationContext();
                                				context.register(TestConfig.class);
                                				context.refresh();

                                				gridFsTemplate = context.getBean(GridFsTemplate.class);

                                				repo = context.getBean(TestEntityRepository.class);
                                				store = context.getBean(TestEntityStore.class);

                                				RandomString random  = new RandomString(5);
                                				resourceLocation = random.nextString();

                                entity = new TestEntity();
                                                        entity = repo.save(entity);

                                genericResource = store.getResource(resourceLocation);

                                store.associate(entity, resourceLocation);
                                                                store.associate(entity, PropertyPath.from("rendition"), resourceLocation);
                            }
                            @AfterEach
                            void tearDown() throws Throwable {
                                context.close();
                            }
                            @Test
                            void shouldThrowAnError() throws Throwable {
                                try {
                                                                        store.unassociate(entity, PropertyPath.from("does.not.exist"));
                                                                    } catch (Exception sae) {
                                                                        MongoStoreIT.this.e = sae;
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
                void setUp() throws Throwable {
                    context = new AnnotationConfigApplicationContext();
                    				context.register(TestConfig.class);
                    				context.refresh();

                    				gridFsTemplate = context.getBean(GridFsTemplate.class);

                    				repo = context.getBean(TestEntityRepository.class);
                    				store = context.getBean(TestEntityStore.class);

                    				RandomString random  = new RandomString(5);
                    				resourceLocation = random.nextString();

                    entity = new TestEntity();
                                        entity = repo.save(entity);

                                        store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                        store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                }
                @AfterEach
                void tearDown() throws Throwable {
                    context.close();
                }
                @Test
                void shouldBeAbleToStoreNewContent() throws Throwable {
                    // content
                                        try (InputStream content = store.getContent(entity)) {
                                            assertThat(IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring Content World!".getBytes()), content)).isTrue();
                                        } catch (IOException ioe) {}

                                        //rendition
                                        try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                                            assertThat(IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring Content World!".getBytes()), content)).isTrue();
                                        } catch (IOException ioe) {}
                }
                @Test
                void shouldHaveContentMetadata() throws Throwable {
                    // content
                                        assertThat(entity.getContentId()).isNotNull();
                                        assertThat(entity.getContentId().trim().length()).isGreaterThan(0);
                    					assertThat(entity.getContentLen()).isEqualTo(27L);

                                        //rendition
                                        assertThat(entity.getRenditionId()).isNotNull();
                                        assertThat(entity.getRenditionId().trim().length()).isGreaterThan(0);
                                        assertThat(entity.getRenditionLen()).isEqualTo(27L);
                }
            }
            @Nested
            class WhenContentIsUpdated {
                @BeforeEach
                void setUp() throws Throwable {
                    context = new AnnotationConfigApplicationContext();
                    				context.register(TestConfig.class);
                    				context.refresh();

                    				gridFsTemplate = context.getBean(GridFsTemplate.class);

                    				repo = context.getBean(TestEntityRepository.class);
                    				store = context.getBean(TestEntityStore.class);

                    				RandomString random  = new RandomString(5);
                    				resourceLocation = random.nextString();

                    entity = new TestEntity();
                                        entity = repo.save(entity);

                                        store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                        store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));

                    store.setContent(entity, new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()));
                                            store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()));
                                            entity = repo.save(entity);
                }
                @AfterEach
                void tearDown() throws Throwable {
                    context.close();
                }
                @Test
                void shouldHaveTheUpdatedContent() throws Throwable {
                    //content
                                            boolean matches = false;
                                            try (InputStream content = store.getContent(entity)) {
                                                matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), content);
                                                assertThat(matches).isTrue();
                                            }

                                            //rendition
                                            matches = false;
                                            try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                                                matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), content);
                                                assertThat(matches).isTrue();
                                            }
                }
            }
            @Nested
            class WhenContentIsUpdatedWithShorterContent {
                @BeforeEach
                void setUp() throws Throwable {
                    context = new AnnotationConfigApplicationContext();
                    				context.register(TestConfig.class);
                    				context.refresh();

                    				gridFsTemplate = context.getBean(GridFsTemplate.class);

                    				repo = context.getBean(TestEntityRepository.class);
                    				store = context.getBean(TestEntityStore.class);

                    				RandomString random  = new RandomString(5);
                    				resourceLocation = random.nextString();

                    entity = new TestEntity();
                                        entity = repo.save(entity);

                                        store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                        store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));

                    store.setContent(entity, new ByteArrayInputStream("Hello Spring World!".getBytes()));
                                            store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("Hello Spring World!".getBytes()));
                                            entity = repo.save(entity);
                }
                @AfterEach
                void tearDown() throws Throwable {
                    context.close();
                }
                @Test
                void shouldStoreOnlyTheNewContent() throws Throwable {
                    //content
                                            boolean matches = false;
                                            try (InputStream content = store.getContent(entity)) {
                                                matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring World!".getBytes()), content);
                                                assertThat(matches).isTrue();
                                            }

                                            //rendition
                                            matches = false;
                                            try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                                                matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring World!".getBytes()), content);
                                                assertThat(matches).isTrue();
                                            }
                }
            }
            @Nested
            class WhenContentIsUpdatedAndNotOverwritten {
                @BeforeEach
                void setUp() throws Throwable {
                    context = new AnnotationConfigApplicationContext();
                    				context.register(TestConfig.class);
                    				context.refresh();

                    				gridFsTemplate = context.getBean(GridFsTemplate.class);

                    				repo = context.getBean(TestEntityRepository.class);
                    				store = context.getBean(TestEntityStore.class);

                    				RandomString random  = new RandomString(5);
                    				resourceLocation = random.nextString();

                    entity = new TestEntity();
                                        entity = repo.save(entity);

                                        store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                        store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                }
                @AfterEach
                void tearDown() throws Throwable {
                    context.close();
                }
                @Test
                void shouldHaveTheUpdatedContent() throws Throwable {
                    String contentId = entity.getContentId();
                    						assertThat(gridFsTemplate.getResource(contentId).exists()).isTrue();

                    						store.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), new SetContentParams(-1, true, SetContentParams.ContentDisposition.CreateNew));
                    						entity = repo.save(entity);

                    						boolean matches = false;
                    						try (InputStream content = store.getContent(entity)) {
                    							matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), content);
                    							assertThat(matches).isTrue();
                    						}

                    						assertThat(gridFsTemplate.getResource(contentId).exists()).isTrue();

                    						assertThat(entity.getContentId()).isNotEqualTo(contentId);

                    						assertThat(gridFsTemplate.getResource(entity.getContentId()).exists()).isTrue();
                }
            }
            @Nested
            class WhenContentIsUnset {
                @BeforeEach
                void setUp() throws Throwable {
                    context = new AnnotationConfigApplicationContext();
                    				context.register(TestConfig.class);
                    				context.refresh();

                    				gridFsTemplate = context.getBean(GridFsTemplate.class);

                    				repo = context.getBean(TestEntityRepository.class);
                    				store = context.getBean(TestEntityStore.class);

                    				RandomString random  = new RandomString(5);
                    				resourceLocation = random.nextString();

                    entity = new TestEntity();
                                        entity = repo.save(entity);

                                        store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                        store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));

                    resourceLocation = entity.getContentId().toString();
                                            entity = store.unsetContent(entity);
                                            entity = store.unsetContent(entity, PropertyPath.from("rendition"));
                                            entity = repo.save(entity);
                }
                @AfterEach
                void tearDown() throws Throwable {
                    context.close();
                }
                @Test
                void shouldHaveNoContent() throws Throwable {
                    //content
                                            try (InputStream content = store.getContent(entity)) {
                                                assertThat(content).isNull();
                                            }

                                            assertThat(entity.getContentId()).isNull();
                    						assertThat(entity.getContentLen()).isNull();

                    						assertThat(gridFsTemplate.getResource(resourceLocation).exists()).isFalse();

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
                void setUp() throws Throwable {
                    context = new AnnotationConfigApplicationContext();
                    				context.register(TestConfig.class);
                    				context.refresh();

                    				gridFsTemplate = context.getBean(GridFsTemplate.class);

                    				repo = context.getBean(TestEntityRepository.class);
                    				store = context.getBean(TestEntityStore.class);

                    				RandomString random  = new RandomString(5);
                    				resourceLocation = random.nextString();

                    entity = new TestEntity();
                                        entity = repo.save(entity);

                                        store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                        store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));

                    resourceLocation = entity.getContentId().toString();
                    						entity = store.unsetContent(entity, PropertyPath.from("content"), new UnsetContentParams(UnsetContentParams.Disposition.Keep));
                    						entity = repo.save(entity);
                }
                @AfterEach
                void tearDown() throws Throwable {
                    context.close();
                }
                @Test
                void shouldHaveNoContent() throws Throwable {
                    //content
                    						try (InputStream content = store.getContent(entity)) {
                    							assertThat(content).isNull();
                    						}

                    						assertThat(entity.getContentId()).isNull();
                    						assertThat(entity.getContentLen()).isNull();

                    						assertThat(gridFsTemplate.getResource(resourceLocation).exists()).isTrue();
                }
            }
            @Nested
            class WhenAnInvalidPropertyPathIsUsedToSetContent {
                @BeforeEach
                void setUp() throws Throwable {
                    context = new AnnotationConfigApplicationContext();
                    				context.register(TestConfig.class);
                    				context.refresh();

                    				gridFsTemplate = context.getBean(GridFsTemplate.class);

                    				repo = context.getBean(TestEntityRepository.class);
                    				store = context.getBean(TestEntityStore.class);

                    				RandomString random  = new RandomString(5);
                    				resourceLocation = random.nextString();

                    entity = new TestEntity();
                                        entity = repo.save(entity);

                                        store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                        store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                }
                @AfterEach
                void tearDown() throws Throwable {
                    context.close();
                }
                @Test
                void shouldThrowAnError() throws Throwable {
                    try {
                                                store.setContent(entity, PropertyPath.from("does.not.exist"), new ByteArrayInputStream("foo".getBytes()));
                                            } catch (Exception sae) {
                                                MongoStoreIT.this.e = sae;
                                            }
                                            assertThat(e).isInstanceOf(StoreAccessException.class);
                }
            }
            @Nested
            class WhenAnInvalidPropertyPathIsUsedToGetContent {
                @BeforeEach
                void setUp() throws Throwable {
                    context = new AnnotationConfigApplicationContext();
                    				context.register(TestConfig.class);
                    				context.refresh();

                    				gridFsTemplate = context.getBean(GridFsTemplate.class);

                    				repo = context.getBean(TestEntityRepository.class);
                    				store = context.getBean(TestEntityStore.class);

                    				RandomString random  = new RandomString(5);
                    				resourceLocation = random.nextString();

                    entity = new TestEntity();
                                        entity = repo.save(entity);

                                        store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                        store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                }
                @AfterEach
                void tearDown() throws Throwable {
                    context.close();
                }
                @Test
                void shouldThrowAnError() throws Throwable {
                    try {
                                                store.getContent(entity, PropertyPath.from("does.not.exist"));
                                            } catch (Exception sae) {
                                                MongoStoreIT.this.e = sae;
                                            }
                                            assertThat(e).isInstanceOf(StoreAccessException.class);
                }
            }
            @Nested
            class WhenAnInvalidPropertyPathIsUsedToUnsetContent {
                @BeforeEach
                void setUp() throws Throwable {
                    context = new AnnotationConfigApplicationContext();
                    				context.register(TestConfig.class);
                    				context.refresh();

                    				gridFsTemplate = context.getBean(GridFsTemplate.class);

                    				repo = context.getBean(TestEntityRepository.class);
                    				store = context.getBean(TestEntityStore.class);

                    				RandomString random  = new RandomString(5);
                    				resourceLocation = random.nextString();

                    entity = new TestEntity();
                                        entity = repo.save(entity);

                                        store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                        store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                }
                @AfterEach
                void tearDown() throws Throwable {
                    context.close();
                }
                @Test
                void shouldThrowAnError() throws Throwable {
                    try {
                                                store.unsetContent(entity, PropertyPath.from("does.not.exist"));
                                            } catch (Exception sae) {
                                                MongoStoreIT.this.e = sae;
                                            }
                                            assertThat(e).isInstanceOf(StoreAccessException.class);
                }
            }
            @Nested
            class WhenContentIsDeletedAndTheIdFieldIsSharedWithJakartaId {
                @BeforeEach
                void setUp() throws Throwable {
                    context = new AnnotationConfigApplicationContext();
                    				context.register(TestConfig.class);
                    				context.refresh();

                    				gridFsTemplate = context.getBean(GridFsTemplate.class);

                    				repo = context.getBean(TestEntityRepository.class);
                    				store = context.getBean(TestEntityStore.class);

                    				RandomString random  = new RandomString(5);
                    				resourceLocation = random.nextString();

                    entity = new TestEntity();
                                        entity = repo.save(entity);

                                        store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                        store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                }
                @AfterEach
                void tearDown() throws Throwable {
                    context.close();
                }
                @Test
                void shouldNotResetTheIdField() throws Throwable {
                    SharedIdRepository sharedIdRepository = context.getBean(SharedIdRepository.class);
                    						SharedIdStore sharedIdStore = context.getBean(SharedIdStore.class);

                    						SharedIdContentIdEntity sharedIdContentIdEntity = sharedIdRepository.save(new SharedIdContentIdEntity());

                    						sharedIdContentIdEntity = sharedIdStore.setContent(sharedIdContentIdEntity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                    						sharedIdContentIdEntity = sharedIdRepository.save(sharedIdContentIdEntity);
                    						String id = sharedIdContentIdEntity.getContentId();
                    						sharedIdContentIdEntity = sharedIdStore.unsetContent(sharedIdContentIdEntity);
                    						assertThat(sharedIdContentIdEntity.getContentId()).isEqualTo(id);
                    						assertThat(sharedIdContentIdEntity.getContentLen()).isNull();
                }
            }
            @Nested
            class WhenContentIsDeletedAndTheIdFieldIsSharedWithSpringId {
                @BeforeEach
                void setUp() throws Throwable {
                    context = new AnnotationConfigApplicationContext();
                    				context.register(TestConfig.class);
                    				context.refresh();

                    				gridFsTemplate = context.getBean(GridFsTemplate.class);

                    				repo = context.getBean(TestEntityRepository.class);
                    				store = context.getBean(TestEntityStore.class);

                    				RandomString random  = new RandomString(5);
                    				resourceLocation = random.nextString();

                    entity = new TestEntity();
                                        entity = repo.save(entity);

                                        store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                        store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                }
                @AfterEach
                void tearDown() throws Throwable {
                    context.close();
                }
                @Test
                void shouldNotResetTheIdField() throws Throwable {
                    SharedSpringIdRepository SharedSpringIdRepository = context.getBean(SharedSpringIdRepository.class);
                    						SharedSpringIdStore SharedSpringIdStore = context.getBean(SharedSpringIdStore.class);

                    						SharedSpringIdContentIdEntity SharedSpringIdContentIdEntity = SharedSpringIdRepository.save(new SharedSpringIdContentIdEntity());

                    						SharedSpringIdContentIdEntity = SharedSpringIdStore.setContent(SharedSpringIdContentIdEntity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                    						SharedSpringIdContentIdEntity = SharedSpringIdRepository.save(SharedSpringIdContentIdEntity);
                    						String id = SharedSpringIdContentIdEntity.getContentId();
                    						SharedSpringIdContentIdEntity = SharedSpringIdStore.unsetContent(SharedSpringIdContentIdEntity);
                    						assertThat(SharedSpringIdContentIdEntity.getContentId()).isEqualTo(id);
                    						assertThat(SharedSpringIdContentIdEntity.getContentLen()).isNull();
                }
            }
        }
    }


	@Test
	public void test() {
		// noop
	}

	@Configuration
	@EnableMongoRepositories(considerNestedRepositories = true)
	@EnableMongoStores
	@Import(InfrastructureConfig.class)
	public static class TestConfig {
		//
	}

	@Configuration
	public static class InfrastructureConfig extends AbstractMongoClientConfiguration {
		@Override
		protected String getDatabaseName() {
			return MongoTestContainer.getTestDbName();
		}

		@Override
        @Bean
		public MongoClient mongoClient() {
			return MongoTestContainer.getMongoClient();
		}

		@Bean
		public GridFsTemplate gridFsTemplate(MappingMongoConverter mongoConverter) {
			return new GridFsTemplate(mongoDbFactory(), mongoConverter);
		}

		@Override
        @Bean
		public MongoDatabaseFactory mongoDbFactory() {
			return new SimpleMongoClientDatabaseFactory(mongoClient(), getDatabaseName());
		}
	}

	public interface ContentProperty {
		String getContentId();

		void setContentId(String contentId);

		Long getContentLen();

		void setContentLen(Long contentLen);
	}

	public static class TestEntity implements ContentProperty {

		@ContentId
		private String contentId;

		@ContentLength
		private Long contentLen;

        @ContentId
        private String renditionId;

        @ContentLength
        private long renditionLen;

		public TestEntity() {
			this.contentId = null;
		}

		public TestEntity(String contentId) {
			this.contentId = new String(contentId);
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
	}

	public interface TestEntityRepository extends MongoRepository<TestEntity, String> {}
	public interface TestEntityStore extends ContentStore<TestEntity, String> {}

	public static class SharedIdContentIdEntity implements ContentProperty {

		@jakarta.persistence.Id
		@ContentId
		private String contentId;

		@ContentLength
		private Long contentLen;

		public SharedIdContentIdEntity() {
			this.contentId = null;
		}

		@Override
		public String getContentId() {
			return contentId;
		}

		@Override
		public void setContentId(String contentId) {
			this.contentId = contentId;
		}

		@Override
		public Long getContentLen() {
			return contentLen;
		}

		@Override
		public void setContentLen(Long contentLen) {
			this.contentLen = contentLen;
		}
	}

	public interface SharedIdRepository extends MongoRepository<SharedIdContentIdEntity, String> {}
	public interface SharedIdStore extends ContentStore<SharedIdContentIdEntity, String> {}

	public static class SharedSpringIdContentIdEntity implements ContentProperty {

		@org.springframework.data.annotation.Id
		@ContentId
		private String contentId;

		@ContentLength
		private Long contentLen;

		public SharedSpringIdContentIdEntity() {
			this.contentId = null;
		}

		@Override
		public String getContentId() {
			return contentId;
		}

		@Override
		public void setContentId(String contentId) {
			this.contentId = contentId;
		}

		@Override
		public Long getContentLen() {
			return contentLen;
		}

		@Override
		public void setContentLen(Long contentLen) {
			this.contentLen = contentLen;
		}
	}

	public interface SharedSpringIdRepository extends MongoRepository<SharedSpringIdContentIdEntity, String> {}
	public interface SharedSpringIdStore extends ContentStore<SharedSpringIdContentIdEntity, String> {}
}
