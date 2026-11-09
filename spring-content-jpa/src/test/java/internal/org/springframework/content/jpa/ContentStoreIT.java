package internal.org.springframework.content.jpa;

import com.github.f4b6a3.uuid.UuidCreator;
import internal.org.springframework.content.jpa.StoreIT.*;
import internal.org.springframework.content.jpa.testsupport.models.Claim;
import internal.org.springframework.content.jpa.testsupport.models.ClaimForm;
import internal.org.springframework.content.jpa.testsupport.repositories.ClaimRepository;
import internal.org.springframework.content.jpa.testsupport.stores.ClaimStore;
import jakarta.persistence.*;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.SetContentParams;
import org.springframework.content.commons.store.UnsetContentParams;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.function.ThrowingSupplier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ContentStoreIT {

    private static final Class<?>[] CONFIG_CLASSES = new Class[]{
            H2Config.class,
            HSQLConfig.class,
            MySqlConfig.class,
            PostgresConfig.class
    };
    protected ClaimRepository claimRepo;
    protected ClaimStore claimFormStore;
    protected Claim claim;
    protected Object id;
    // for Postgres (large object api operations must be in a transaction)
    private PlatformTransactionManager ptm;
    private EmbeddedRepository embeddedRepo;
    private EmbeddedStore embeddedStore;

    public static <T> T doInTransaction(PlatformTransactionManager ptm, ThrowingSupplier<T> block) {
        TransactionStatus status = ptm.getTransaction(new DefaultTransactionDefinition());

        try {
            T result = block.getWithException();
            ptm.commit(status);
            return result;
        } catch (Exception e) {
            ptm.rollback(status);
        }

        return null;
    }

    protected boolean hasContent(Claim claim, PropertyPath path) {

        if (claim == null) {
            return false;
        }

        return Boolean.TRUE.equals(doInTransaction(ptm, () -> {
            try (InputStream content = claimFormStore.getContent(claim, path)) {
                if (content != null) {
                    return true;
                }
            } catch (Exception ignored) {
            }
            return false;
        }));
    }

    protected void deleteAllClaims() {
        claimRepo.deleteAll();
    }

    protected void deleteAllClaimFormsContent() {
        Iterable<Claim> existingClaims = claimRepo.findAll();
        for (Claim existingClaim : existingClaims) {
            if (existingClaim.getClaimForm() != null && (hasContent(existingClaim, PropertyPath.from("claimForm/content")) || hasContent(existingClaim, PropertyPath.from("claimForm/rendition")))) {
                claimFormStore.unsetContent(existingClaim, PropertyPath.from("claimForm/content"));
                claimFormStore.unsetContent(existingClaim, PropertyPath.from("claimForm/rendition"));
                if (existingClaim.getClaimForm() != null) {
                    assertThat(existingClaim.getClaimForm().getContentId()).isNull();
                    assertThat(existingClaim.getClaimForm().getContentLength()).isNull();
                    assertThat(existingClaim.getClaimForm().getRenditionId()).isNull();
                    assertThat(existingClaim.getClaimForm().getRenditionLen()).isEqualTo(0L);

                    // double-check the content got removed
                    InputStream content = doInTransaction(ptm, () -> claimFormStore.getContent(existingClaim, PropertyPath.from("claimForm/content")));
                    InputStream renditionContent = doInTransaction(ptm, () -> claimFormStore.getContent(existingClaim, PropertyPath.from("claimForm/rendition")));
                    try {
                        assertThat(content).isNull();
                        assertThat(renditionContent).isNull();
                    } finally {
                        IOUtils.closeQuietly(content);
                    }
                }
            }
        }
    }

    public interface EmbeddedRepository extends JpaRepository<EntityWithEmbeddedContent, String> {
    }

    public interface EmbeddedStore extends ContentStore<EntityWithEmbeddedContent, String> {
    }

    @Entity
    @Table(name = "entity_with_embedded")
    public static class EntityWithEmbeddedContent {

        @Id
        private String id = UuidCreator.getTimeOrdered().toString();

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

    static Stream<Class<?>> databaseConfigs() {
        return Stream.of(CONFIG_CLASSES);
    }

    private void boot(Class<?> configClass) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(TestConfig.class);
        context.register(configClass);
        context.refresh();

        ptm = context.getBean(PlatformTransactionManager.class);
        claimRepo = context.getBean(ClaimRepository.class);
        claimFormStore = context.getBean(ClaimStore.class);

        embeddedRepo = context.getBean(EmbeddedRepository.class);
        embeddedStore = context.getBean(EmbeddedStore.class);

        if (ptm == null) {
            ptm = mock(PlatformTransactionManager.class);
        }

        claim = new Claim();
        claim.setFirstName("John");
        claim.setLastName("Smith");
        claim.setClaimForm(new ClaimForm());
        claim = claimRepo.save(claim);
    }

    private void setInitialContent() {
        claimFormStore.setContent(claim, PropertyPath.from("claimForm/content"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
        claimFormStore.setContent(claim, PropertyPath.from("claimForm/rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));
    }

    @Nested
    class GivenAnEntityWithContent {
        @ParameterizedTest(name = "{0}")
        @MethodSource("internal.org.springframework.content.jpa.ContentStoreIT#databaseConfigs")
        void shouldBeAbleToStoreNewContent(Class<?> configClass) {
            try {
                boot(configClass);
                setInitialContent();
                // content
                doInTransaction(ptm, () -> {
                    try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/content"))) {
                        assertThat(IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring Content World!".getBytes()), content)).isTrue();
                    } catch (IOException ignored) {
                    }
                    return null;
                });

                // rendition
                doInTransaction(ptm, () -> {
                    try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/rendition"))) {
                        assertThat(IOUtils.contentEquals(new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()), content)).isTrue();
                    } catch (IOException ignored) {
                    }
                    return null;
                });
            } finally {
                deleteAllClaimFormsContent();
                deleteAllClaims();
            }
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("internal.org.springframework.content.jpa.ContentStoreIT#databaseConfigs")
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
            @MethodSource("internal.org.springframework.content.jpa.ContentStoreIT#databaseConfigs")
            void shouldHaveTheUpdatedContent(Class<?> configClass) {
                try {
                    boot(configClass);
                    setInitialContent();

                    claimFormStore.setContent(claim, PropertyPath.from("claimForm/content"), new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()));
                    claimFormStore.setContent(claim, PropertyPath.from("claimForm/rendition"), new ByteArrayInputStream("<html>Hello Updated Spring Content World!</html>".getBytes()));
                    claim = claimRepo.save(claim);
                    // content
                    doInTransaction(ptm, () -> {
                        try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/content"))) {
                            boolean matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), content);
                            assertThat(matches).isTrue();
                        } catch (IOException ignored) {
                        }
                        return null;
                    });

                    // rendition
                    doInTransaction(ptm, () -> {
                        try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/rendition"))) {
                            boolean matches = IOUtils.contentEquals(new ByteArrayInputStream("<html>Hello Updated Spring Content World!</html>".getBytes()), content);
                            assertThat(matches).isTrue();
                        } catch (IOException ignored) {
                        }
                        return null;
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
            @MethodSource("internal.org.springframework.content.jpa.ContentStoreIT#databaseConfigs")
            void shouldStoreOnlyTheNewContent(Class<?> configClass) {
                try {
                    boot(configClass);
                    setInitialContent();

                    claimFormStore.setContent(claim, PropertyPath.from("claimForm/content"), new ByteArrayInputStream("Hello Spring World!".getBytes()));
                    claimFormStore.setContent(claim, PropertyPath.from("claimForm/rendition"), new ByteArrayInputStream("<html>Hello Spring World!</html>".getBytes()));
                    claim = claimRepo.save(claim);
                    // content
                    doInTransaction(ptm, () -> {
                        try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/content"))) {
                            boolean matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring World!".getBytes()), content);
                            assertThat(matches).isTrue();
                        } catch (IOException ignored) {
                        }
                        return null;
                    });

                    // rendition
                    doInTransaction(ptm, () -> {
                        try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/rendition"))) {
                            boolean matches = IOUtils.contentEquals(new ByteArrayInputStream("<html>Hello Spring World!</html>".getBytes()), content);
                            assertThat(matches).isTrue();
                        } catch (IOException ignored) {
                        }
                        return null;
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
            @MethodSource("internal.org.springframework.content.jpa.ContentStoreIT#databaseConfigs")
            void shouldHaveTheUpdatedContent(Class<?> configClass) throws IOException {
                try {
                    boot(configClass);
                    setInitialContent();
                    String contentId = claim.getClaimForm().getContentId();

                    claimFormStore.setContent(claim, PropertyPath.from("claimForm/content"), new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), new SetContentParams(-1, true, SetContentParams.ContentDisposition.CreateNew));
                    claim = claimRepo.save(claim);

                    try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/content"))) {
                        boolean matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), content);
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
            @MethodSource("internal.org.springframework.content.jpa.ContentStoreIT#databaseConfigs")
            void shouldHaveNoContent(Class<?> configClass) {
                try {
                    boot(configClass);
                    setInitialContent();

                    id = claim.getClaimForm().getContentId();
                    claimFormStore.unsetContent(claim, PropertyPath.from("claimForm/content"));
                    claimFormStore.unsetContent(claim, PropertyPath.from("claimForm/rendition"));
                    claim = claimRepo.save(claim);
                    ClaimForm deletedClaimForm = new ClaimForm();
                    deletedClaimForm.setContentId((String) id);

                    // content
                    doInTransaction(ptm, () -> {
                        try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/content"))) {
                            assertThat(content).isNull();
                        } catch (IOException ignored) {
                        }
                        return null;
                    });

                    assertThat(claim.getClaimForm().getContentId()).isNull();
                    assertThat(claim.getClaimForm().getContentLength()).isNull();

                    // rendition
                    doInTransaction(ptm, () -> {
                        try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/rendition"))) {
                            assertThat(content).isNull();
                        } catch (IOException ignored) {
                        }
                        return null;
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
            @MethodSource("internal.org.springframework.content.jpa.ContentStoreIT#databaseConfigs")
            void shouldHaveNoContent(Class<?> configClass) {
                try {
                    boot(configClass);
                    setInitialContent();

                    id = claim.getClaimForm().getContentId();
                    claimFormStore.unsetContent(claim, PropertyPath.from("claimForm/content"), new UnsetContentParams(UnsetContentParams.Disposition.Keep));
                    claim = claimRepo.save(claim);
                    ClaimForm deletedClaimForm = new ClaimForm();
                    deletedClaimForm.setContentId((String) id);

                    // content
                    doInTransaction(ptm, () -> {
                        try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/content"))) {
                            assertThat(content).isNull();
                        } catch (IOException ignored) {
                        }
                        return null;
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
                @MethodSource("internal.org.springframework.content.jpa.ContentStoreIT#databaseConfigs")
                void shouldReturnNullWhenContentIsFetched(Class<?> configClass) throws IOException {
                    try {
                        boot(configClass);
                        setInitialContent();
                        EntityWithEmbeddedContent entity = embeddedRepo.save(new EntityWithEmbeddedContent());
                        assertThat(embeddedStore.getContent(entity, PropertyPath.from("content"))).isNull();
                    } finally {
                        deleteAllClaimFormsContent();
                        deleteAllClaims();
                    }
                }

                @ParameterizedTest(name = "{0}")
                @MethodSource("internal.org.springframework.content.jpa.ContentStoreIT#databaseConfigs")
                void shouldBeSuccessfulWhenContentIsSet(Class<?> configClass) throws IOException {
                    try {
                        boot(configClass);
                        setInitialContent();
                        EntityWithEmbeddedContent entity = embeddedRepo.save(new EntityWithEmbeddedContent());
                        embeddedStore.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                        try (InputStream is = embeddedStore.getContent(entity, PropertyPath.from("content"))) {
                            assertThat(IOUtils.contentEquals(is, new ByteArrayInputStream("Hello Spring Content World!".getBytes()))).isTrue();
                        }
                    } finally {
                        deleteAllClaimFormsContent();
                        deleteAllClaims();
                    }
                }

                @ParameterizedTest(name = "{0}")
                @MethodSource("internal.org.springframework.content.jpa.ContentStoreIT#databaseConfigs")
                void shouldReturnNullWhenContentIsUnset(Class<?> configClass) {
                    try {
                        boot(configClass);
                        setInitialContent();
                        EntityWithEmbeddedContent entity = embeddedRepo.save(new EntityWithEmbeddedContent());
                        assertThat(embeddedStore.unsetContent(entity, PropertyPath.from("content"))).isSameAs(entity);
                    } finally {
                        deleteAllClaimFormsContent();
                        deleteAllClaims();
                    }
                }
            }
        }
    }
}
