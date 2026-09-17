package internal.org.springframework.content.jpa;

import com.github.f4b6a3.uuid.UuidCreator;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import internal.org.springframework.content.jpa.StoreIT.*;
import internal.org.springframework.content.jpa.testsupport.models.Claim;
import internal.org.springframework.content.jpa.testsupport.models.ClaimForm;
import internal.org.springframework.content.jpa.testsupport.repositories.ClaimRepository;
import internal.org.springframework.content.jpa.testsupport.stores.ClaimStore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.runner.RunWith;
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

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static internal.org.springframework.content.jpa.StoreIT.getContextName;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class ContentStoreIT {

    private static final Class<?>[] CONFIG_CLASSES = new Class[]{
            H2Config.class,
            HSQLConfig.class,
            MySqlConfig.class,
            PostgresConfig.class
    };

    private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    // for postgres (large object api operations must be in a transaction)
    private PlatformTransactionManager ptm;

    protected ClaimRepository claimRepo;
    protected ClaimStore claimFormStore;

    private EmbeddedRepository embeddedRepo;
    private EmbeddedStore embeddedStore;

    protected Claim claim;
    protected Object id;

    {
        Describe("ContentStore", () -> {

            for (Class<?> configClass : CONFIG_CLASSES) {

                Context(getContextName(configClass), () -> {

                    BeforeEach(() -> {
                        context = new AnnotationConfigApplicationContext();
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
                    });

                    AfterEach(() -> {
                        deleteAllClaimFormsContent();
                        deleteAllClaims();
                    });

                    Context("given an Entity with content", () -> {

                        BeforeEach(() -> {
                            claim = new Claim();
                            claim.setFirstName("John");
                            claim.setLastName("Smith");
                            claim.setClaimForm(new ClaimForm());
                            claim = claimRepo.save(claim);

                            claimFormStore.setContent(claim, PropertyPath.from("claimForm/content"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                            claimFormStore.setContent(claim, PropertyPath.from("claimForm/rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));
                        });

                        It("should be able to store new content", () -> {
                            // content
                            doInTransaction(ptm, () -> {
                                try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/content"))) {
                                    assertThat(IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring Content World!".getBytes()), content), is(true));
                                } catch (IOException ignored) {
                                }
                                return null;
                            });

                            // rendition
                            doInTransaction(ptm, () -> {
                                try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/rendition"))) {
                                    assertThat(IOUtils.contentEquals(new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()), content), is(true));
                                } catch (IOException ignored) {
                                }
                                return null;
                            });

                        });

                        It("should have content metadata", () -> {
                            // content
                            Assert.assertNotNull(claim.getClaimForm().getContentId());
                            Assert.assertTrue(claim.getClaimForm().getContentId().trim().length() > 0);
                            Assert.assertEquals(27L, (long) claim.getClaimForm().getContentLength());

                            // rendition
                            Assert.assertNotNull(claim.getClaimForm().getRenditionId());
                            Assert.assertTrue(claim.getClaimForm().getRenditionId().trim().length() > 0);
                            Assert.assertEquals(40L, (long) claim.getClaimForm().getRenditionLen());
                        });

                        Context("when content is updated", () -> {
                            BeforeEach(() -> {
                                claimFormStore.setContent(claim, PropertyPath.from("claimForm/content"), new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()));
                                claimFormStore.setContent(claim, PropertyPath.from("claimForm/rendition"), new ByteArrayInputStream("<html>Hello Updated Spring Content World!</html>".getBytes()));
                                claim = claimRepo.save(claim);
                            });

                            It("should have the updated content", () -> {
                                // content
                                doInTransaction(ptm, () -> {
                                    try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/content"))) {
                                        boolean matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), content);
                                        assertThat(matches, is(true));
                                    } catch (IOException ignored) {
                                    }
                                    return null;
                                });

                                // rendition
                                doInTransaction(ptm, () -> {
                                    try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/rendition"))) {
                                        boolean matches = IOUtils.contentEquals(new ByteArrayInputStream("<html>Hello Updated Spring Content World!</html>".getBytes()), content);
                                        assertThat(matches, is(true));
                                    } catch (IOException ignored) {
                                    }
                                    return null;
                                });
                            });
                        });

                        Context("when content is updated with shorter content", () -> {
                            BeforeEach(() -> {
                                claimFormStore.setContent(claim, PropertyPath.from("claimForm/content"), new ByteArrayInputStream("Hello Spring World!".getBytes()));
                                claimFormStore.setContent(claim, PropertyPath.from("claimForm/rendition"), new ByteArrayInputStream("<html>Hello Spring World!</html>".getBytes()));
                                claim = claimRepo.save(claim);
                            });
                            It("should store only the new content", () -> {
                                // content
                                doInTransaction(ptm, () -> {
                                    try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/content"))) {
                                        boolean matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring World!".getBytes()), content);
                                        assertThat(matches, is(true));
                                    } catch (IOException ignored) {
                                    }
                                    return null;
                                });

                                // rendition
                                doInTransaction(ptm, () -> {
                                    try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/rendition"))) {
                                        boolean matches = IOUtils.contentEquals(new ByteArrayInputStream("<html>Hello Spring World!</html>".getBytes()), content);
                                        assertThat(matches, is(true));
                                    } catch (IOException ignored) {
                                    }
                                    return null;
                                });
                            });
                        });

                        Context("when content is updated and not overwritten", () ->
                                It("should have the updated content", () -> {
                                    String contentId = claim.getClaimForm().getContentId();

                                    claimFormStore.setContent(claim, PropertyPath.from("claimForm/content"), new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), new SetContentParams(-1, true, SetContentParams.ContentDisposition.CreateNew));
                                    claim = claimRepo.save(claim);

                                    try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/content"))) {
                                        boolean matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), content);
                                        assertThat(matches, is(true));
                                    }

                                    assertThat(claim.getClaimForm().getContentId(), is(not(contentId)));
                                })
                        );

                        Context("when content is deleted", () -> {
                            BeforeEach(() -> {
                                id = claim.getClaimForm().getContentId();
                                claimFormStore.unsetContent(claim, PropertyPath.from("claimForm/content"));
                                claimFormStore.unsetContent(claim, PropertyPath.from("claimForm/rendition"));
                                claim = claimRepo.save(claim);
                            });

                            AfterEach(() -> claimRepo.delete(claim));

                            It("should have no content", () -> {
                                ClaimForm deletedClaimForm = new ClaimForm();
                                deletedClaimForm.setContentId((String) id);

                                // content
                                doInTransaction(ptm, () -> {
                                    try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/content"))) {
                                        Assert.assertNull(content);
                                    } catch (IOException ignored) {
                                    }
                                    return null;
                                });

                                Assert.assertNull(claim.getClaimForm().getContentId());
                                Assert.assertNull(claim.getClaimForm().getContentLength());

                                // rendition
                                doInTransaction(ptm, () -> {
                                    try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/rendition"))) {
                                        Assert.assertNull(content);
                                    } catch (IOException ignored) {
                                    }
                                    return null;
                                });

                                Assert.assertNull(claim.getClaimForm().getRenditionId());
                                Assert.assertEquals(0L, claim.getClaimForm().getRenditionLen());
                            });
                        });

                        Context("when content is deleted", () -> {
                            BeforeEach(() -> {
                                id = claim.getClaimForm().getContentId();
                                claimFormStore.unsetContent(claim, PropertyPath.from("claimForm/content"), new UnsetContentParams(UnsetContentParams.Disposition.Keep));
                                claim = claimRepo.save(claim);
                            });

                            AfterEach(() -> claimRepo.delete(claim));

                            It("should have no content", () -> {
                                ClaimForm deletedClaimForm = new ClaimForm();
                                deletedClaimForm.setContentId((String) id);

                                // content
                                doInTransaction(ptm, () -> {
                                    try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/content"))) {
                                        Assert.assertNull(content);
                                    } catch (IOException ignored) {
                                    }
                                    return null;
                                });

                                Assert.assertNull(claim.getClaimForm().getContentId());
                                Assert.assertNull(claim.getClaimForm().getContentLength());
                            });
                        });

                        Context("@Embedded content", () -> Context("given a entity with a null embedded content object", () -> {
                            It("should return null when content is fetched", () -> {
                                EntityWithEmbeddedContent entity = embeddedRepo.save(new EntityWithEmbeddedContent());
                                assertThat(embeddedStore.getContent(entity, PropertyPath.from("content")), is(nullValue()));
                            });

                            It("should be successful when content is set", () -> {
                                EntityWithEmbeddedContent entity = embeddedRepo.save(new EntityWithEmbeddedContent());
                                embeddedStore.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                try (InputStream is = embeddedStore.getContent(entity, PropertyPath.from("content"))) {
                                    assertThat(IOUtils.contentEquals(is, new ByteArrayInputStream("Hello Spring Content World!".getBytes())), is(true));
                                }
                            });

                            It("should return null when content is unset", () -> {
                                EntityWithEmbeddedContent entity = embeddedRepo.save(new EntityWithEmbeddedContent());
                                EntityWithEmbeddedContent expected = new EntityWithEmbeddedContent(entity.getId(), entity.getContent());
                                assertThat(embeddedStore.unsetContent(entity, PropertyPath.from("content")), is(expected));
                            });
                        }));
                    });
                });
            }
        });
    }

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
                    Assert.assertNull(existingClaim.getClaimForm().getContentId());
                    Assert.assertNull(existingClaim.getClaimForm().getContentLength());
                    Assert.assertNull(existingClaim.getClaimForm().getRenditionId());
                    Assert.assertEquals(0L, existingClaim.getClaimForm().getRenditionLen());

                    // double-check the content got removed
                    InputStream content = doInTransaction(ptm, () -> claimFormStore.getContent(existingClaim, PropertyPath.from("claimForm/content")));
                    InputStream renditionContent = doInTransaction(ptm, () -> claimFormStore.getContent(existingClaim, PropertyPath.from("claimForm/rendition")));
                    try {
                        Assert.assertNull(content);
                        Assert.assertNull(renditionContent);
                    } finally {
                        IOUtils.closeQuietly(content);
                    }
                }
            }
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Entity
    @Table(name = "entity_with_embedded")
    public static class EntityWithEmbeddedContent {

        @Id
        private String id = UuidCreator.getTimeOrdered().toString();

        @Embedded
        private EmbeddedContent content;
    }

    @Embeddable
    @NoArgsConstructor
    @Data
    public static class EmbeddedContent {

        @ContentId
        private String contentId;

        @ContentLength
        private Long contentLen;
    }

    public interface EmbeddedRepository extends JpaRepository<EntityWithEmbeddedContent, String> {
    }

    public interface EmbeddedStore extends ContentStore<EntityWithEmbeddedContent, String> {
    }
}
