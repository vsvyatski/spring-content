package internal.org.springframework.content.jpa;

import com.github.f4b6a3.uuid.UuidCreator;
import internal.org.springframework.content.jpa.StoreIT.H2Config;
import internal.org.springframework.content.jpa.StoreIT.HSQLConfig;
import internal.org.springframework.content.jpa.StoreIT.MySqlConfig;
import internal.org.springframework.content.jpa.StoreIT.PostgresConfig;
import internal.org.springframework.content.jpa.testsupport.models.Document;
import internal.org.springframework.content.jpa.testsupport.repositories.DocumentRepository;
import internal.org.springframework.content.jpa.testsupport.stores.DocumentAssociativeStore;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.GetResourceParams;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class AssociativeStoreIT {

    private static final Class<?>[] CONFIG_CLASSES = new Class[]{
            H2Config.class,
            HSQLConfig.class,
            MySqlConfig.class,
            PostgresConfig.class
    };

    private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    private DocumentRepository repo;
    private DocumentAssociativeStore store;

    private PlatformTransactionManager txn;

    private Document document;
    private Resource resource;
    private String resourceId;

    private Exception e;

    static Stream<Class<?>> databaseConfigs() {
        return Stream.of(CONFIG_CLASSES);
    }

    private void boot(Class<?> configClass) {
        context = new AnnotationConfigApplicationContext();
        context.register(StoreIT.TestConfig.class);
        context.register(configClass);
        context.refresh();

        repo = context.getBean(DocumentRepository.class);
        store = context.getBean(DocumentAssociativeStore.class);
        txn = context.getBean(PlatformTransactionManager.class);
    }

    private void saveNewDocument() {
        document = new Document();
        document = repo.save(document);
    }

    private void prepareResource() {
        resourceId = UuidCreator.getTimeOrdered().toString();
        resource = store.getResource(resourceId);
    }

    private void associateContentAndRendition() {
        store.associate(document, resourceId);
        store.associate(document, PropertyPath.from("rendition"), resourceId);
    }

    @Nested
    class GivenANewEntity {
        @ParameterizedTest(name = "{0}")
        @MethodSource("internal.org.springframework.content.jpa.AssociativeStoreIT#databaseConfigs")
        void shouldNotHaveAnAssociatedResource(Class<?> configClass) {
            boot(configClass);
            saveNewDocument();
            assertThat(document.getContentId()).isNull();
            assertThat(store.getResource(document)).isNull();
        }

        @Nested
        class GivenAResource {
            @Nested
            class WhenTheResourceIsAssociated {
                @ParameterizedTest(name = "{0}")
                @MethodSource("internal.org.springframework.content.jpa.AssociativeStoreIT#databaseConfigs")
                void shouldBeRecordedAsSuchOnTheEntitysContentId(Class<?> configClass) {
                    boot(configClass);
                    saveNewDocument();
                    prepareResource();

                    associateContentAndRendition();
                    assertThat(document.getContentId()).isEqualTo(resourceId);
                    assertThat(document.getRenditionId()).isEqualTo(resourceId);
                }

                @Nested
                class WhenTheResourceHasContent {
                    @ParameterizedTest(name = "{0}")
                    @MethodSource("internal.org.springframework.content.jpa.AssociativeStoreIT#databaseConfigs")
                    void shouldNotHonorByteRanges(Class<?> configClass) throws IOException {
                        boot(configClass);
                        saveNewDocument();
                        prepareResource();
                        associateContentAndRendition();

                        TransactionStatus status = txn.getTransaction(new DefaultTransactionDefinition());

                        try (OutputStream os = ((WritableResource) resource).getOutputStream()) {
                            os.write("Hello Client-side World!".getBytes());
                        }

                        txn.commit(status);
                        // relies on REST-layer to serve byte range
                        Resource r = store.getResource(document, PropertyPath.from("content"), new GetResourceParams("5-10"));
                        try (InputStream is = r.getInputStream()) {
                            assertThat(IOUtils.toString(is, Charset.defaultCharset())).isEqualTo("Hello Client-side World!");
                        }
                    }
                }

                @Nested
                class WhenTheResourceIsUnassociated {
                    @ParameterizedTest(name = "{0}")
                    @MethodSource("internal.org.springframework.content.jpa.AssociativeStoreIT#databaseConfigs")
                    void shouldResetTheEntitysContentId(Class<?> configClass) {
                        boot(configClass);
                        saveNewDocument();
                        prepareResource();
                        associateContentAndRendition();

                        store.unassociate(document);
                        store.unassociate(document, PropertyPath.from("rendition"));
                        assertThat(document.getContentId()).isNull();
                        assertThat(document.getRenditionId()).isNull();
                    }
                }
            }

            @Nested
            class WhenAnInvalidPropertyPathIsUsedToAssociateAResource {
                @ParameterizedTest(name = "{0}")
                @MethodSource("internal.org.springframework.content.jpa.AssociativeStoreIT#databaseConfigs")
                void shouldThrowAnError(Class<?> configClass) {
                    boot(configClass);
                    saveNewDocument();
                    prepareResource();
                    try {
                        store.associate(document, PropertyPath.from("does.not.exist"), resourceId);
                    } catch (Exception sae) {
                        AssociativeStoreIT.this.e = sae;
                    }
                    assertThat(e).isInstanceOf(StoreAccessException.class);
                }
            }

            @Nested
            class WhenAnInvalidPropertyPathIsUsedToLoadAResource {
                @ParameterizedTest(name = "{0}")
                @MethodSource("internal.org.springframework.content.jpa.AssociativeStoreIT#databaseConfigs")
                void shouldThrowAnError(Class<?> configClass) {
                    boot(configClass);
                    saveNewDocument();
                    prepareResource();
                    try {
                        store.getResource(document, PropertyPath.from("does.not.exist"));
                    } catch (Exception sae) {
                        AssociativeStoreIT.this.e = sae;
                    }
                    assertThat(e).isInstanceOf(StoreAccessException.class);
                }
            }

            @Nested
            class WhenAnInvalidPropertyPathIsUsedToUnassociateAResource {
                @ParameterizedTest(name = "{0}")
                @MethodSource("internal.org.springframework.content.jpa.AssociativeStoreIT#databaseConfigs")
                void shouldThrowAnError(Class<?> configClass) {
                    boot(configClass);
                    saveNewDocument();
                    prepareResource();
                    try {
                        store.unassociate(document, PropertyPath.from("does.not.exist"));
                    } catch (Exception sae) {
                        AssociativeStoreIT.this.e = sae;
                    }
                    assertThat(e).isInstanceOf(StoreAccessException.class);
                }
            }
        }
    }
}
