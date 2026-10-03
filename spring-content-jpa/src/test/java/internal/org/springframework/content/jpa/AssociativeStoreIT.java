package internal.org.springframework.content.jpa;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestFactory;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.f4b6a3.uuid.UuidCreator;
import internal.org.springframework.content.jpa.StoreIT.H2Config;
import internal.org.springframework.content.jpa.StoreIT.HSQLConfig;
import internal.org.springframework.content.jpa.StoreIT.MySqlConfig;
import internal.org.springframework.content.jpa.StoreIT.PostgresConfig;
import internal.org.springframework.content.jpa.testsupport.models.Document;
import internal.org.springframework.content.jpa.testsupport.repositories.DocumentRepository;
import internal.org.springframework.content.jpa.testsupport.stores.DocumentAssociativeStore;
import org.apache.commons.io.IOUtils;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.GetResourceParams;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import static internal.org.springframework.content.jpa.StoreIT.getContextName;

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

    
    @Nested
    class AssociativeStoreCases {
        @TestFactory
        java.util.stream.Stream<org.junit.jupiter.api.DynamicNode> generatedCases() {
            java.util.List<org.junit.jupiter.api.DynamicNode> tests = new java.util.ArrayList<>();
            for (Class<?> configClass : CONFIG_CLASSES) {
                {
                    java.util.List<org.junit.jupiter.api.DynamicNode> nodes9 = new java.util.ArrayList<>();
                    {
                        java.util.List<org.junit.jupiter.api.DynamicNode> nodes8 = new java.util.ArrayList<>();
                        nodes8.add(org.junit.jupiter.api.DynamicTest.dynamicTest("should not have an associated resource", () -> {
                            context = new AnnotationConfigApplicationContext();
                                                    context.register(StoreIT.TestConfig.class);
                                                    context.register(configClass);
                                                    context.refresh();

                                                    repo = context.getBean(DocumentRepository.class);
                                                    store = context.getBean(DocumentAssociativeStore.class);
                                                    txn = context.getBean(PlatformTransactionManager.class);

                            document = new Document();
                                                        document = repo.save(document);
                            assertThat(document.getContentId()).isNull();
                                                        assertThat(store.getResource(document)).isNull();
                        }));
                        {
                            java.util.List<org.junit.jupiter.api.DynamicNode> nodes7 = new java.util.ArrayList<>();
                            {
                                java.util.List<org.junit.jupiter.api.DynamicNode> nodes3 = new java.util.ArrayList<>();
                                nodes3.add(org.junit.jupiter.api.DynamicTest.dynamicTest("should be recorded as such on the entity's @ContentId", () -> {
                                    context = new AnnotationConfigApplicationContext();
                                                            context.register(StoreIT.TestConfig.class);
                                                            context.register(configClass);
                                                            context.refresh();

                                                            repo = context.getBean(DocumentRepository.class);
                                                            store = context.getBean(DocumentAssociativeStore.class);
                                                            txn = context.getBean(PlatformTransactionManager.class);

                                    document = new Document();
                                                                document = repo.save(document);

                                    resourceId = UuidCreator.getTimeOrdered().toString();
                                                                    resource = store.getResource(resourceId);

                                    store.associate(document, resourceId);
                                                                        store.associate(document, PropertyPath.from("rendition"), resourceId);
                                    assertThat(document.getContentId()).isEqualTo(resourceId);
                                                                        assertThat(document.getRenditionId()).isEqualTo(resourceId);
                                }));
                                {
                                    java.util.List<org.junit.jupiter.api.DynamicNode> nodes1 = new java.util.ArrayList<>();
                                    nodes1.add(org.junit.jupiter.api.DynamicTest.dynamicTest("should not honor byte ranges", () -> {
                                        context = new AnnotationConfigApplicationContext();
                                                                context.register(StoreIT.TestConfig.class);
                                                                context.register(configClass);
                                                                context.refresh();

                                                                repo = context.getBean(DocumentRepository.class);
                                                                store = context.getBean(DocumentAssociativeStore.class);
                                                                txn = context.getBean(PlatformTransactionManager.class);

                                        document = new Document();
                                                                    document = repo.save(document);

                                        resourceId = UuidCreator.getTimeOrdered().toString();
                                                                        resource = store.getResource(resourceId);

                                        store.associate(document, resourceId);
                                                                            store.associate(document, PropertyPath.from("rendition"), resourceId);

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
                                    }));
                                    nodes1.add(org.junit.jupiter.api.DynamicContainer.dynamicContainer("when the resource has content", nodes1.stream()));
                                }
                                {
                                    java.util.List<org.junit.jupiter.api.DynamicNode> nodes2 = new java.util.ArrayList<>();
                                    nodes2.add(org.junit.jupiter.api.DynamicTest.dynamicTest("should reset the entity's @ContentId", () -> {
                                        context = new AnnotationConfigApplicationContext();
                                                                context.register(StoreIT.TestConfig.class);
                                                                context.register(configClass);
                                                                context.refresh();

                                                                repo = context.getBean(DocumentRepository.class);
                                                                store = context.getBean(DocumentAssociativeStore.class);
                                                                txn = context.getBean(PlatformTransactionManager.class);

                                        document = new Document();
                                                                    document = repo.save(document);

                                        resourceId = UuidCreator.getTimeOrdered().toString();
                                                                        resource = store.getResource(resourceId);

                                        store.associate(document, resourceId);
                                                                            store.associate(document, PropertyPath.from("rendition"), resourceId);

                                        store.unassociate(document);
                                                                                store.unassociate(document, PropertyPath.from("rendition"));
                                        assertThat(document.getContentId()).isNull();
                                                                                assertThat(document.getRenditionId()).isNull();
                                    }));
                                    nodes2.add(org.junit.jupiter.api.DynamicContainer.dynamicContainer("when the resource is unassociated", nodes2.stream()));
                                }
                                nodes3.add(org.junit.jupiter.api.DynamicContainer.dynamicContainer("when the resource is associated", nodes3.stream()));
                            }
                            {
                                java.util.List<org.junit.jupiter.api.DynamicNode> nodes4 = new java.util.ArrayList<>();
                                nodes4.add(org.junit.jupiter.api.DynamicTest.dynamicTest("should throw an error", () -> {
                                    context = new AnnotationConfigApplicationContext();
                                                            context.register(StoreIT.TestConfig.class);
                                                            context.register(configClass);
                                                            context.refresh();

                                                            repo = context.getBean(DocumentRepository.class);
                                                            store = context.getBean(DocumentAssociativeStore.class);
                                                            txn = context.getBean(PlatformTransactionManager.class);

                                    document = new Document();
                                                                document = repo.save(document);

                                    resourceId = UuidCreator.getTimeOrdered().toString();
                                                                    resource = store.getResource(resourceId);
                                    try {
                                                                                store.associate(document, PropertyPath.from("does.not.exist"), resourceId);
                                                                            } catch (Exception sae) {
                                                                                AssociativeStoreIT.this.e = sae;
                                                                            }
                                                                            assertThat(e).isInstanceOf(StoreAccessException.class);
                                }));
                                nodes4.add(org.junit.jupiter.api.DynamicContainer.dynamicContainer("when a invalid property path is used to associate a resource", nodes4.stream()));
                            }
                            {
                                java.util.List<org.junit.jupiter.api.DynamicNode> nodes5 = new java.util.ArrayList<>();
                                nodes5.add(org.junit.jupiter.api.DynamicTest.dynamicTest("should throw an error", () -> {
                                    context = new AnnotationConfigApplicationContext();
                                                            context.register(StoreIT.TestConfig.class);
                                                            context.register(configClass);
                                                            context.refresh();

                                                            repo = context.getBean(DocumentRepository.class);
                                                            store = context.getBean(DocumentAssociativeStore.class);
                                                            txn = context.getBean(PlatformTransactionManager.class);

                                    document = new Document();
                                                                document = repo.save(document);

                                    resourceId = UuidCreator.getTimeOrdered().toString();
                                                                    resource = store.getResource(resourceId);
                                    try {
                                                                                store.getResource(document, PropertyPath.from("does.not.exist"));
                                                                            } catch (Exception sae) {
                                                                                AssociativeStoreIT.this.e = sae;
                                                                            }
                                                                            assertThat(e).isInstanceOf(StoreAccessException.class);
                                }));
                                nodes5.add(org.junit.jupiter.api.DynamicContainer.dynamicContainer("when a invalid property path is used to load a resource", nodes5.stream()));
                            }
                            {
                                java.util.List<org.junit.jupiter.api.DynamicNode> nodes6 = new java.util.ArrayList<>();
                                nodes6.add(org.junit.jupiter.api.DynamicTest.dynamicTest("should throw an error", () -> {
                                    context = new AnnotationConfigApplicationContext();
                                                            context.register(StoreIT.TestConfig.class);
                                                            context.register(configClass);
                                                            context.refresh();

                                                            repo = context.getBean(DocumentRepository.class);
                                                            store = context.getBean(DocumentAssociativeStore.class);
                                                            txn = context.getBean(PlatformTransactionManager.class);

                                    document = new Document();
                                                                document = repo.save(document);

                                    resourceId = UuidCreator.getTimeOrdered().toString();
                                                                    resource = store.getResource(resourceId);
                                    try {
                                                                                store.unassociate(document, PropertyPath.from("does.not.exist"));
                                                                            } catch (Exception sae) {
                                                                                AssociativeStoreIT.this.e = sae;
                                                                            }
                                                                            assertThat(e).isInstanceOf(StoreAccessException.class);
                                }));
                                nodes6.add(org.junit.jupiter.api.DynamicContainer.dynamicContainer("when a invalid property path is used to unassociate a resource", nodes6.stream()));
                            }
                            nodes7.add(org.junit.jupiter.api.DynamicContainer.dynamicContainer("given a resource", nodes7.stream()));
                        }
                        nodes8.add(org.junit.jupiter.api.DynamicContainer.dynamicContainer("given a new entity", nodes8.stream()));
                    }
                    tests.add(org.junit.jupiter.api.DynamicContainer.dynamicContainer(getContextName(configClass), nodes9.stream()));
                }
            }
            return tests.stream();
        }
    }

}
