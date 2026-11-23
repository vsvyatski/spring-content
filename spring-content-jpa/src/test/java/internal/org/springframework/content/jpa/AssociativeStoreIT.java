package internal.org.springframework.content.jpa;

import com.github.f4b6a3.uuid.UuidCreator;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.GetResourceParams;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.test.support.jpa.DualContentMimeJpaAssociativeStore;
import org.springframework.content.test.support.jpa.DualContentMimeJpaStringIdEntity;
import org.springframework.content.test.support.jpa.DualContentMimeJpaStringIdRepository;
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

import static org.assertj.core.api.Assertions.assertThat;

public class AssociativeStoreIT {

    private DualContentMimeJpaStringIdRepository repo;
    private DualContentMimeJpaAssociativeStore store;

    private PlatformTransactionManager txn;

    private DualContentMimeJpaStringIdEntity document;
    private Resource resource;
    private String resourceId;

    private Exception e;

    private void boot(Class<?> configClass) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(StoreIT.TestConfig.class);
        context.register(configClass);
        context.refresh();

        repo = context.getBean(DualContentMimeJpaStringIdRepository.class);
        store = context.getBean(DualContentMimeJpaAssociativeStore.class);
        txn = context.getBean(PlatformTransactionManager.class);
    }

    private void saveNewDocument() {
        document = repo.save(new DualContentMimeJpaStringIdEntity());
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
    @DisplayName("Given a new entity")
    class NewEntity {
        @ParameterizedTest(name = "{0}")
        @MethodSource("internal.org.springframework.content.jpa.StoreIT#databaseConfigs")
        void shouldNotHaveAssociatedResource(Class<?> configClass) {
            boot(configClass);
            saveNewDocument();
            assertThat(document.getContentId()).isNull();
            ContentStoreIT.assertNullResource(store.getResource(document));
        }

        @Nested
        @DisplayName("Given a resource")
        class AssociatedResource {
            @Nested
            @DisplayName("When the resource is associated")
            class ResourceIsAssociated {
                @ParameterizedTest(name = "{0}")
                @MethodSource("internal.org.springframework.content.jpa.StoreIT#databaseConfigs")
                void shouldBeRecordedAsSuchOnEntityContentId(Class<?> configClass) {
                    boot(configClass);
                    saveNewDocument();
                    prepareResource();

                    associateContentAndRendition();
                    assertThat(document.getContentId()).isEqualTo(resourceId);
                    assertThat(document.getRenditionId()).isEqualTo(resourceId);
                }

                @Nested
                @DisplayName("When the resource has content")
                class ResourceHasContent {
                    @ParameterizedTest(name = "{0}")
                    @MethodSource("internal.org.springframework.content.jpa.StoreIT#databaseConfigs")
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
                @DisplayName("When the resource is unassociated")
                class ResourceIsUnassociated {
                    @ParameterizedTest(name = "{0}")
                    @MethodSource("internal.org.springframework.content.jpa.StoreIT#databaseConfigs")
                    void shouldResetEntityContentId(Class<?> configClass) {
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
            @DisplayName("When an invalid property path is used to associate a resource")
            class InvalidPropertyPathIsUsedToAssociateResource {
                @ParameterizedTest(name = "{0}")
                @MethodSource("internal.org.springframework.content.jpa.StoreIT#databaseConfigs")
                void shouldThrowError(Class<?> configClass) {
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
            @DisplayName("When an invalid property path is used to load a resource")
            class InvalidPropertyPathIsUsedToLoadResource {
                @ParameterizedTest(name = "{0}")
                @MethodSource("internal.org.springframework.content.jpa.StoreIT#databaseConfigs")
                void shouldThrowError(Class<?> configClass) {
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
            @DisplayName("When an invalid property path is used to unassociate a resource")
            class InvalidPropertyPathIsUsedToUnassociateResource {
                @ParameterizedTest(name = "{0}")
                @MethodSource("internal.org.springframework.content.jpa.StoreIT#databaseConfigs")
                void shouldThrowError(Class<?> configClass) {
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
