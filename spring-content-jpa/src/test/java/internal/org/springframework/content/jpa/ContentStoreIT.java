package internal.org.springframework.content.jpa;

import org.apache.commons.io.IOUtils;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.SetContentParams;
import org.springframework.content.commons.store.UnsetContentParams;
import org.springframework.content.test.support.NullableAssertions;
import org.springframework.content.test.support.TestSupportData;
import org.springframework.content.test.support.jpa.*;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.function.ThrowingSupplier;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ContentStoreIT {

    protected NestedDualContentJpaStringIdRepository claimRepo;
    protected NestedDualContentJpaStringIdStore claimFormStore;
    protected NestedDualContentJpaStringIdEntity claim;
    protected Object id;
    // for Postgres (large object api operations must be in a transaction)
    private PlatformTransactionManager ptm;
    private AnnotationConfigApplicationContext context;

    public static <T> T doInTransaction(PlatformTransactionManager ptm, ThrowingSupplier<T> block) {
        TransactionStatus status = ptm.getTransaction(new DefaultTransactionDefinition());

        try {
            T result = block.getWithException();
            ptm.commit(status);
            return result;
        } catch (Exception e) {
            ptm.rollback(status);
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(e);
        }
    }

    public static void runInTransaction(PlatformTransactionManager ptm, ThrowingRunnable block) {
        TransactionStatus status = ptm.getTransaction(new DefaultTransactionDefinition());
        try {
            block.run();
            ptm.commit(status);
        } catch (Exception e) {
            ptm.rollback(status);
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * ContentStore#getContent may return null; call through here for null-marked assertions.
     */
    public static void assertNullInputStream(@Nullable InputStream content) {
        NullableAssertions.assertNull(content);
    }

    /**
     * AssociativeStore#getResource may return null; call through here for null-marked assertions.
     */
    public static void assertNullResource(@Nullable Resource resource) {
        NullableAssertions.assertNull(resource);
    }

    protected boolean hasContent(NestedDualContentJpaStringIdEntity claim, PropertyPath path) {

        if (claim == null) {
            return false;
        }

        return doInTransaction(ptm, () -> {
            try (InputStream content = claimFormStore.getContent(claim, path)) {
                if (content != null) {
                    return true;
                }
            } catch (Exception ignored) {
            }
            return false;
        });
    }

    protected void deleteAllClaims() {
        claimRepo.deleteAll();
    }

    protected void deleteAllClaimFormsContent() {
        Iterable<NestedDualContentJpaStringIdEntity> existingClaims = claimRepo.findAll();
        for (NestedDualContentJpaStringIdEntity existingClaim : existingClaims) {
            if (existingClaim.getClaimForm() != null && (hasContent(existingClaim, PropertyPath.from("claimForm/content")) || hasContent(existingClaim, PropertyPath.from("claimForm/rendition")))) {
                claimFormStore.unsetContent(existingClaim, PropertyPath.from("claimForm/content"));
                claimFormStore.unsetContent(existingClaim, PropertyPath.from("claimForm/rendition"));
                if (existingClaim.getClaimForm() != null) {
                    assertThat(existingClaim.getClaimForm().getContentId()).isNull();
                    assertThat(existingClaim.getClaimForm().getContentLength()).isNull();
                    assertThat(existingClaim.getClaimForm().getRenditionId()).isNull();
                    assertThat(existingClaim.getClaimForm().getRenditionLen()).isEqualTo(0L);

                    // double-check the content got removed
                    InputStream content = doInTransaction(ptm,
                        () -> claimFormStore.getContent(existingClaim, PropertyPath.from("claimForm/content")));
                    InputStream renditionContent = doInTransaction(ptm,
                        () -> claimFormStore.getContent(existingClaim, PropertyPath.from("claimForm/rendition")));
                    try {
                        assertNullInputStream(content);
                        assertNullInputStream(renditionContent);
                    } finally {
                        IOUtils.closeQuietly(content);
                    }
                }
            }
        }
    }

    private void boot(Class<?> configClass) {
        context = new AnnotationConfigApplicationContext();
        context.register(StoreIT.TestConfig.class);
        context.register(configClass);
        context.refresh();

        ptm = context.getBean(PlatformTransactionManager.class);
        claimRepo = context.getBean(NestedDualContentJpaStringIdRepository.class);
        claimFormStore = context.getBean(NestedDualContentJpaStringIdStore.class);

        if (ptm == null) {
            ptm = mock(PlatformTransactionManager.class);
        }

        claim = new NestedDualContentJpaStringIdEntity();
        claim.setFirstName("John");
        claim.setLastName("Smith");
        claim.setClaimForm(new NestedDualContentJpaEmbeddable());
        claim = claimRepo.save(claim);
    }

    private void setInitialContent() {
        claimFormStore.setContent(claim, PropertyPath.from("claimForm/content"), TestSupportData.sampleTextStream());
        claimFormStore.setContent(claim, PropertyPath.from("claimForm/rendition"), TestSupportData.sampleHtmlRenditionStream());
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }

    @Nested
    class GivenAnEntityWithContent {
        @ParameterizedTest(name = "{0}")
        @MethodSource("internal.org.springframework.content.jpa.StoreIT#databaseConfigs")
        void shouldBeAbleToStoreNewContent(Class<?> configClass) {
            try {
                boot(configClass);
                setInitialContent();
                // content
                runInTransaction(ptm, () -> {
                    try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/content"))) {
                        assertThat(IOUtils.contentEquals(TestSupportData.sampleTextStream(), content)).isTrue();
                    } catch (IOException ignored) {
                    }
                });

                // rendition
                runInTransaction(ptm, () -> {
                    try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/rendition"))) {
                        assertThat(IOUtils.contentEquals(TestSupportData.sampleHtmlRenditionStream(), content)).isTrue();
                    } catch (IOException ignored) {
                    }
                });
            } finally {
                deleteAllClaimFormsContent();
                deleteAllClaims();
            }
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("internal.org.springframework.content.jpa.StoreIT#databaseConfigs")
        void shouldHaveContentMetadata(Class<?> configClass) {
            try {
                boot(configClass);
                setInitialContent();
                // content
                assertThat(claim.getClaimForm().getContentId()).isNotNull();
                assertThat(!claim.getClaimForm().getContentId().trim().isEmpty()).isTrue();
                assertThat(claim.getClaimForm().getContentLength()).isEqualTo(27L);

                // rendition
                assertThat(claim.getClaimForm().getRenditionId()).isNotNull();
                assertThat(!claim.getClaimForm().getRenditionId().trim().isEmpty()).isTrue();
                assertThat(40L).isEqualTo(claim.getClaimForm().getRenditionLen());
            } finally {
                deleteAllClaimFormsContent();
                deleteAllClaims();
            }
        }

        @Nested
        class WhenContentIsUpdated {
            @ParameterizedTest(name = "{0}")
            @MethodSource("internal.org.springframework.content.jpa.StoreIT#databaseConfigs")
            void shouldHaveTheUpdatedContent(Class<?> configClass) {
                try {
                    boot(configClass);
                    setInitialContent();

                    claimFormStore.setContent(claim, PropertyPath.from("claimForm/content"), TestSupportData.sampleUpdatedTextStream());
                    claimFormStore.setContent(claim, PropertyPath.from("claimForm/rendition"), TestSupportData.sampleUpdatedHtmlRenditionStream());
                    claim = claimRepo.save(claim);
                    // content
                    runInTransaction(ptm, () -> {
                        try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/content"))) {
                            boolean matches = IOUtils.contentEquals(TestSupportData.sampleUpdatedTextStream(), content);
                            assertThat(matches).isTrue();
                        } catch (IOException ignored) {
                        }
                    });

                    // rendition
                    runInTransaction(ptm, () -> {
                        try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/rendition"))) {
                            boolean matches = IOUtils.contentEquals(TestSupportData.sampleUpdatedHtmlRenditionStream(), content);
                            assertThat(matches).isTrue();
                        } catch (IOException ignored) {
                        }
                    });
                } finally {
                    deleteAllClaimFormsContent();
                    deleteAllClaims();
                }
            }
        }

        @Nested
        class WhenContentIsUpdatedWithShorterContent {
            @ParameterizedTest(name = "{0}")
            @MethodSource("internal.org.springframework.content.jpa.StoreIT#databaseConfigs")
            void shouldStoreOnlyTheNewContent(Class<?> configClass) {
                try {
                    boot(configClass);
                    setInitialContent();

                    claimFormStore.setContent(claim, PropertyPath.from("claimForm/content"), TestSupportData.samplePartialUpdateStream());
                    claimFormStore.setContent(claim, PropertyPath.from("claimForm/rendition"), TestSupportData.samplePartialUpdateHtmlRenditionStream());
                    claim = claimRepo.save(claim);
                    // content
                    runInTransaction(ptm, () -> {
                        try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/content"))) {
                            boolean matches = IOUtils.contentEquals(TestSupportData.samplePartialUpdateStream(), content);
                            assertThat(matches).isTrue();
                        } catch (IOException ignored) {
                        }
                    });

                    // rendition
                    runInTransaction(ptm, () -> {
                        try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/rendition"))) {
                            boolean matches = IOUtils.contentEquals(TestSupportData.samplePartialUpdateHtmlRenditionStream(), content);
                            assertThat(matches).isTrue();
                        } catch (IOException ignored) {
                        }
                    });
                } finally {
                    deleteAllClaimFormsContent();
                    deleteAllClaims();
                }
            }
        }

        @Nested
        class WhenContentIsUpdatedAndNotOverwritten {
            @ParameterizedTest(name = "{0}")
            @MethodSource("internal.org.springframework.content.jpa.StoreIT#databaseConfigs")
            void shouldHaveTheUpdatedContent(Class<?> configClass) throws IOException {
                try {
                    boot(configClass);
                    setInitialContent();
                    String contentId = claim.getClaimForm().getContentId();

                    claimFormStore.setContent(claim, PropertyPath.from("claimForm/content"), TestSupportData.sampleUpdatedTextStream(), new SetContentParams(-1, true, SetContentParams.ContentDisposition.CreateNew));
                    claim = claimRepo.save(claim);

                    try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/content"))) {
                        boolean matches = IOUtils.contentEquals(TestSupportData.sampleUpdatedTextStream(), content);
                        assertThat(matches).isTrue();
                    }

                    assertThat(claim.getClaimForm().getContentId()).isNotEqualTo(contentId);
                } finally {
                    deleteAllClaimFormsContent();
                    deleteAllClaims();
                }
            }
        }

        @Nested
        class WhenContentIsDeleted {
            @ParameterizedTest(name = "{0}")
            @MethodSource("internal.org.springframework.content.jpa.StoreIT#databaseConfigs")
            void shouldHaveNoContent(Class<?> configClass) {
                try {
                    boot(configClass);
                    setInitialContent();

                    id = claim.getClaimForm().getContentId();
                    claimFormStore.unsetContent(claim, PropertyPath.from("claimForm/content"));
                    claimFormStore.unsetContent(claim, PropertyPath.from("claimForm/rendition"));
                    claim = claimRepo.save(claim);
                    NestedDualContentJpaEmbeddable deletedClaimForm = new NestedDualContentJpaEmbeddable();
                    deletedClaimForm.setContentId((String) id);

                    // content
                    runInTransaction(ptm, () -> {
                        try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/content"))) {
                            assertNullInputStream(content);
                        } catch (IOException ignored) {
                        }
                    });

                    assertThat(claim.getClaimForm().getContentId()).isNull();
                    assertThat(claim.getClaimForm().getContentLength()).isNull();

                    // rendition
                    runInTransaction(ptm, () -> {
                        try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/rendition"))) {
                            assertNullInputStream(content);
                        } catch (IOException ignored) {
                        }
                    });

                    assertThat(claim.getClaimForm().getRenditionId()).isNull();
                    assertThat(0L).isEqualTo(claim.getClaimForm().getRenditionLen());
                } finally {
                    claimRepo.delete(claim);

                    deleteAllClaimFormsContent();
                    deleteAllClaims();
                }
            }
        }

        @Nested
        class WhenContentIsDeletedWithKeepDisposition {
            @ParameterizedTest(name = "{0}")
            @MethodSource("internal.org.springframework.content.jpa.StoreIT#databaseConfigs")
            void shouldHaveNoContent(Class<?> configClass) {
                try {
                    boot(configClass);
                    setInitialContent();

                    id = claim.getClaimForm().getContentId();
                    claimFormStore.unsetContent(claim, PropertyPath.from("claimForm/content"), new UnsetContentParams(UnsetContentParams.Disposition.Keep));
                    claim = claimRepo.save(claim);
                    NestedDualContentJpaEmbeddable deletedClaimForm = new NestedDualContentJpaEmbeddable();
                    deletedClaimForm.setContentId((String) id);

                    // content
                    runInTransaction(ptm, () -> {
                        try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/content"))) {
                            assertNullInputStream(content);
                        } catch (IOException ignored) {
                        }
                    });

                    assertThat(claim.getClaimForm().getContentId()).isNull();
                    assertThat(claim.getClaimForm().getContentLength()).isNull();
                } finally {
                    claimRepo.delete(claim);

                    deleteAllClaimFormsContent();
                    deleteAllClaims();
                }
            }
        }

        @Nested
        class EmbeddedContent {
            @Nested
            class GivenAnEntityWithANullEmbeddedContentObject {
                @ParameterizedTest(name = "{0}")
                @MethodSource("internal.org.springframework.content.jpa.StoreIT#databaseConfigs")
                void shouldReturnNullWhenContentIsFetched(Class<?> configClass) throws IOException {
                    try {
                        boot(configClass);
                        setInitialContent();
                        EmbeddedContentJpaStringIdRepository embeddedRepo = context.getBean(EmbeddedContentJpaStringIdRepository.class);
                        EmbeddedContentJpaStringIdStore embeddedStore = context.getBean(EmbeddedContentJpaStringIdStore.class);
                        EmbeddedContentJpaStringIdEntity embedded = embeddedRepo.save(new EmbeddedContentJpaStringIdEntity());
                        assertNullInputStream(embeddedStore.getContent(embedded, PropertyPath.from("content")));
                    } finally {
                        deleteAllClaimFormsContent();
                        deleteAllClaims();
                    }
                }

                @ParameterizedTest(name = "{0}")
                @MethodSource("internal.org.springframework.content.jpa.StoreIT#databaseConfigs")
                void shouldBeSuccessfulWhenContentIsSet(Class<?> configClass) throws IOException {
                    try {
                        boot(configClass);
                        setInitialContent();
                        EmbeddedContentJpaStringIdRepository embeddedRepo = context.getBean(EmbeddedContentJpaStringIdRepository.class);
                        EmbeddedContentJpaStringIdStore embeddedStore = context.getBean(EmbeddedContentJpaStringIdStore.class);
                        EmbeddedContentJpaStringIdEntity embedded = embeddedRepo.save(new EmbeddedContentJpaStringIdEntity());
                        embeddedStore.setContent(embedded, PropertyPath.from("content"), TestSupportData.sampleTextStream());
                        try (InputStream is = embeddedStore.getContent(embedded, PropertyPath.from("content"))) {
                            assertThat(IOUtils.contentEquals(is, TestSupportData.sampleTextStream())).isTrue();
                        }
                    } finally {
                        deleteAllClaimFormsContent();
                        deleteAllClaims();
                    }
                }

                @ParameterizedTest(name = "{0}")
                @MethodSource("internal.org.springframework.content.jpa.StoreIT#databaseConfigs")
                void shouldReturnNullWhenContentIsUnset(Class<?> configClass) {
                    try {
                        boot(configClass);
                        setInitialContent();
                        EmbeddedContentJpaStringIdRepository embeddedRepo = context.getBean(EmbeddedContentJpaStringIdRepository.class);
                        EmbeddedContentJpaStringIdStore embeddedStore = context.getBean(EmbeddedContentJpaStringIdStore.class);
                        EmbeddedContentJpaStringIdEntity embedded = embeddedRepo.save(new EmbeddedContentJpaStringIdEntity());
                        assertThat(embeddedStore.unsetContent(embedded, PropertyPath.from("content"))).isSameAs(embedded);
                    } finally {
                        deleteAllClaimFormsContent();
                        deleteAllClaims();
                    }
                }
            }
        }
    }
}
