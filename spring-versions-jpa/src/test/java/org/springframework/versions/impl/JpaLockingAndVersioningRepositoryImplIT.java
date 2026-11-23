package org.springframework.versions.impl;

import internal.org.springframework.versions.LockingService;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.content.test.support.EmbeddedJpa;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.versions.LockOwnerException;
import org.springframework.versions.LockingAndVersioningException;
import org.springframework.versions.VersionInfo;
import org.springframework.versions.jpa.config.JpaLockingAndVersioningConfig;
import org.springframework.versions.test.support.VersioningTestPackages;
import org.springframework.versions.test.support.jpa.VersioningJpaLongIdEntity;
import org.springframework.versions.test.support.jpa.VersioningJpaLongIdRepository;
import org.springframework.versions.test.support.jpa.VersioningJpaSiblingEntity;
import org.springframework.versions.test.support.jpa.VersioningJpaSiblingRepository;

import javax.sql.DataSource;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ContextConfiguration(classes = {JpaLockingAndVersioningRepositoryImplIT.TestConfig.class})
@ExtendWith(SpringExtension.class)
public class JpaLockingAndVersioningRepositoryImplIT {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private LockingService lockingService;

    private VersioningJpaLongIdRepository repo;

    private VersioningJpaLongIdEntity e1, e2, e3, e1v11, e1v12, e2v2, e3wc, entityForDeletion, result;

    private Exception e;

    // this entity and repository ensure that multiple repositories can implement LockingAndVersioningRepository at the same time
    @Autowired
    private VersioningJpaSiblingRepository otherRepo;

    private static void setupSecurityContext(String principal, boolean isAuthenticated) {
        SecurityContext sc = SecurityContextHolder.createEmptyContext();
        sc.setAuthentication(new MockAuthentication(principal, isAuthenticated));
        SecurityContextHolder.setContext(sc);
    }

    @Configuration
    @EnableJpaRepositories(basePackages = VersioningTestPackages.JPA)
    @Import({H2Config.class, JpaLockingAndVersioningConfig.class})
    public static class TestConfig {
    }

    @Configuration
    @EnableTransactionManagement
    public static class H2Config {

        @Value("/org/springframework/versions/jpa/schema-drop-h2.sql")
        private ClassPathResource dropVersionSchema;
        @Value("/org/springframework/versions/jpa/schema-h2.sql")
        private ClassPathResource createVersionSchema;

        @Bean
        public DataSource dataSource() {
            return EmbeddedJpa.embeddedDataSource(EmbeddedDatabaseType.H2);
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
            return EmbeddedJpa.entityManagerFactory(dataSource(), Database.H2, VersioningTestPackages.JPA);
        }

        @Bean
        public PlatformTransactionManager transactionManager() {
            return EmbeddedJpa.transactionManager(entityManagerFactory().getObject());
        }

        @Bean
        public DataSourceInitializer datasourceInitializer() {
            return EmbeddedJpa.schemaInitializer(dataSource(), dropVersionSchema, createVersionSchema);
        }
    }

    private record MockAuthentication(String principal, boolean isAuthenticated) implements Authentication {

        @Override
        public String getName() {
            return principal;
        }

        @Override
        @NonNull
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return Collections.emptyList();
        }

        @Override
        public Object getCredentials() {
            return null;
        }

        @Override
        public Object getDetails() {
            return null;
        }

        @Override
        public Object getPrincipal() {
            return principal;
        }

        @Override
        public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        }
    }

    @Nested

    @DisplayName("Given a locking and versioning repository and a security context")

    class LockingAndVersioningRepositoryAndASecurityContext {
        @Nested
        @DisplayName("Given two entities with two versions each")
        class TwoEntitiesWithTwoVersionsEach {
            @Nested
           class Lock {
                @Nested
                @DisplayName("Given a null principal")
                class ullPrincipal {
                    @BeforeEach
                    void setUp() {
                        repo = context.getBean(VersioningJpaLongIdRepository.class);

                        e1 = new VersioningJpaLongIdEntity();
                        e2 = new VersioningJpaLongIdEntity();

                        // ensure the other instantiate fragment is valid
                        otherRepo.save(new VersioningJpaSiblingEntity());

                        setupSecurityContext("some-principal", true);

                        setupSecurityContext(null, false);

                        try {
                            result = repo.lock(e1);
                        } catch (Exception e) {
                            JpaLockingAndVersioningRepositoryImplIT.this.e = e;
                        }
                    }

                    @Test
                    void shouldThrowASecurityException() {
                        assertThat(e).isInstanceOf(SecurityException.class);
                    }
                }

                @Nested

                @DisplayName("Given the entity is new")

                class EntityIsNew {
                    @BeforeEach
                    void setUp() {
                        repo = context.getBean(VersioningJpaLongIdRepository.class);

                        e1 = new VersioningJpaLongIdEntity();
                        e2 = new VersioningJpaLongIdEntity();

                        // ensure the other instantiate fragment is valid
                        otherRepo.save(new VersioningJpaSiblingEntity());

                        setupSecurityContext("some-principal", true);

                        try {
                            result = repo.lock(e1);
                        } catch (Exception e) {
                            JpaLockingAndVersioningRepositoryImplIT.this.e = e;
                        }
                    }

                    @Test
                    void shouldFail() {
                        assertThat(e).isInstanceOf(InvalidDataAccessApiUsageException.class);
                    }
                }

                @Nested

                @DisplayName("Given the entity exists")

                class EntityExists {
                    @Nested
                    @DisplayName("When the object is not locked")
                    class ObjectIsNotLocked {
                        @BeforeEach
                        void setUp() {
                            repo = context.getBean(VersioningJpaLongIdRepository.class);

                            e1 = new VersioningJpaLongIdEntity();
                            e2 = new VersioningJpaLongIdEntity();

                            // ensure the other instantiate fragment is valid
                            otherRepo.save(new VersioningJpaSiblingEntity());

                            setupSecurityContext("some-principal", true);

                            e1 = repo.save(e1);

                            try {
                                result = repo.lock(e1);
                            } catch (Exception e) {
                                JpaLockingAndVersioningRepositoryImplIT.this.e = e;
                            }
                        }

                        @Test
                        void shouldUpdateTheEntitySLockOwnerField() {
                            assertThat(e1.getXLockOwner()).isEqualTo("some-principal");
                        }

                        @Test
                        void shouldSaveTheEntity() {
                            assertThat(e).isNull();
                            assertThat(result.getXid()).isEqualTo(e1.getXid());
                        }
                    }

                    @Nested

                    @DisplayName("When the lock is already taken")

                    class LockIsAlreadyTaken {
                        @BeforeEach
                        void setUp() {
                            repo = context.getBean(VersioningJpaLongIdRepository.class);

                            e1 = new VersioningJpaLongIdEntity();
                            e2 = new VersioningJpaLongIdEntity();

                            // ensure the other instantiate fragment is valid
                            otherRepo.save(new VersioningJpaSiblingEntity());

                            setupSecurityContext("some-principal", true);

                            e1 = repo.save(e1);

                            setupSecurityContext("some-other-principal", true);
                            e1 = repo.lock(e1);
                            setupSecurityContext("some-principal", true);

                            try {
                                result = repo.lock(e1);
                            } catch (Exception e) {
                                JpaLockingAndVersioningRepositoryImplIT.this.e = e;
                            }
                        }

                        @Test
                        void shouldReturnNull() {
                            assertThat(e).isInstanceOf(LockOwnerException.class);
                            assertThat(result).isNull();
                        }
                    }

                    @Nested

                    @DisplayName("When the lock is already held")

                    class LockIsAlreadyHeld {
                        @BeforeEach
                        void setUp() {
                            repo = context.getBean(VersioningJpaLongIdRepository.class);

                            e1 = new VersioningJpaLongIdEntity();
                            e2 = new VersioningJpaLongIdEntity();

                            // ensure the other instantiate fragment is valid
                            otherRepo.save(new VersioningJpaSiblingEntity());

                            setupSecurityContext("some-principal", true);

                            e1 = repo.save(e1);

                            setupSecurityContext("some-principal", true);
                            e1 = repo.lock(e1);
                            setupSecurityContext("some-principal", true);

                            try {
                                result = repo.lock(e1);
                            } catch (Exception e) {
                                JpaLockingAndVersioningRepositoryImplIT.this.e = e;
                            }
                        }

                        @Test
                        void shouldSucceed() {
                            assertThat(e).isNull();
                            assertThat(result).isNotNull();
                        }
                    }

                    @Nested

                    @DisplayName("When the principal is not authenticated")

                    class PrincipalIsNotAuthenticated {
                        @BeforeEach
                        void setUp() {
                            repo = context.getBean(VersioningJpaLongIdRepository.class);

                            e1 = new VersioningJpaLongIdEntity();
                            e2 = new VersioningJpaLongIdEntity();

                            // ensure the other instantiate fragment is valid
                            otherRepo.save(new VersioningJpaSiblingEntity());

                            setupSecurityContext("some-principal", true);

                            e1 = repo.save(e1);

                            setupSecurityContext("some-principal", false);

                            try {
                                result = repo.lock(e1);
                            } catch (Exception e) {
                                JpaLockingAndVersioningRepositoryImplIT.this.e = e;
                            }
                        }

                        @Test
                        void shouldReturnSecurityException() {
                            assertThat(e).isInstanceOf(SecurityException.class);
                        }
                    }
                }
            }

            @Nested
           class Unlock {
                @Nested
                @DisplayName("Given a null principal")
                class ullPrincipal {
                    @BeforeEach
                    void setUp() {
                        repo = context.getBean(VersioningJpaLongIdRepository.class);

                        e1 = new VersioningJpaLongIdEntity();
                        e2 = new VersioningJpaLongIdEntity();

                        // ensure the other instantiate fragment is valid
                        otherRepo.save(new VersioningJpaSiblingEntity());

                        setupSecurityContext("some-principal", true);

                        setupSecurityContext(null, false);

                        try {
                            result = repo.unlock(e1);
                        } catch (Exception e) {
                            JpaLockingAndVersioningRepositoryImplIT.this.e = e;
                        }
                    }

                    @Test
                    void shouldThrowASecurityException() {
                        assertThat(e).isInstanceOf(SecurityException.class);
                    }
                }

                @Nested

                @DisplayName("Given the entity is new")

                class EntityIsNew {
                    @BeforeEach
                    void setUp() {
                        repo = context.getBean(VersioningJpaLongIdRepository.class);

                        e1 = new VersioningJpaLongIdEntity();
                        e2 = new VersioningJpaLongIdEntity();

                        // ensure the other instantiate fragment is valid
                        otherRepo.save(new VersioningJpaSiblingEntity());

                        setupSecurityContext("some-principal", true);

                        try {
                            result = repo.unlock(e1);
                        } catch (Exception e) {
                            JpaLockingAndVersioningRepositoryImplIT.this.e = e;
                        }
                    }

                    @Test
                    void shouldFail() {
                        assertThat(e).isInstanceOf(InvalidDataAccessApiUsageException.class);
                    }
                }

                @Nested

                @DisplayName("Given the entity exists")

                class EntityExists {
                    @Nested
                    @DisplayName("Given the principal is the lock owner")
                    class PrincipalIsTheLockOwner {
                        @BeforeEach
                        void setUp() {
                            repo = context.getBean(VersioningJpaLongIdRepository.class);

                            e1 = new VersioningJpaLongIdEntity();
                            e2 = new VersioningJpaLongIdEntity();

                            // ensure the other instantiate fragment is valid
                            otherRepo.save(new VersioningJpaSiblingEntity());

                            setupSecurityContext("some-principal", true);

                            e1 = repo.save(e1);

                            setupSecurityContext("some-principal", true);
                            e1 = repo.lock(e1);

                            try {
                                result = repo.unlock(e1);
                            } catch (Exception e) {
                                JpaLockingAndVersioningRepositoryImplIT.this.e = e;
                            }
                        }

                        @Test
                        void shouldNullTheLockOwnerFieldAndSave() {
                            assertThat(result.getXLockOwner()).isNull();
                        }

                        @Test
                        void shouldUnlockTheEntityAndReturnIt() {
                            assertThat(e).isNull();
                            assertThat(result.getXid()).isEqualTo(e1.getXid());
                        }
                    }

                    @Nested

                    @DisplayName("Given the principal is not the lock owner")

                    class PrincipalIsNotTheLockOwner {
                        @BeforeEach
                        void setUp() {
                            repo = context.getBean(VersioningJpaLongIdRepository.class);

                            e1 = new VersioningJpaLongIdEntity();
                            e2 = new VersioningJpaLongIdEntity();

                            // ensure the other instantiate fragment is valid
                            otherRepo.save(new VersioningJpaSiblingEntity());

                            setupSecurityContext("some-principal", true);

                            e1 = repo.save(e1);

                            setupSecurityContext("some-other-principal", true);
                            e1 = repo.lock(e1);
                            setupSecurityContext("some-principal", true);

                            try {
                                result = repo.unlock(e1);
                            } catch (Exception e) {
                                JpaLockingAndVersioningRepositoryImplIT.this.e = e;
                            }
                        }

                        @Test
                        void shouldASecurityException() {
                            assertThat(e).isInstanceOf(LockOwnerException.class);
                            assertThat(e.getMessage()).contains("not lock owner");
                        }
                    }

                    @Nested

                    @DisplayName("Given the principal is not authenticated")

                    class PrincipalIsNotAuthenticated {
                        @BeforeEach
                        void setUp() {
                            repo = context.getBean(VersioningJpaLongIdRepository.class);

                            e1 = new VersioningJpaLongIdEntity();
                            e2 = new VersioningJpaLongIdEntity();

                            // ensure the other instantiate fragment is valid
                            otherRepo.save(new VersioningJpaSiblingEntity());

                            setupSecurityContext("some-principal", true);

                            e1 = repo.save(e1);

                            setupSecurityContext("some-principal", false);

                            try {
                                result = repo.unlock(e1);
                            } catch (Exception e) {
                                JpaLockingAndVersioningRepositoryImplIT.this.e = e;
                            }
                        }

                        @Test
                        void shouldASecurityException() {
                            assertThat(e).isInstanceOf(SecurityException.class);
                        }
                    }
                }
            }

            @Nested
           class Save {
                @Nested
                @DisplayName("Given an authenticated principal")
                class AuthenticatedPrincipal {
                    @Nested
                    @DisplayName("Given the entity is new")
                    class EntityIsNew {
                        @Nested
                        @DisplayName("Given there is no lock owner")
                        class ThereIsNoLockOwner {
                            @BeforeEach
                            void setUp() {
                                repo = context.getBean(VersioningJpaLongIdRepository.class);

                                e1 = new VersioningJpaLongIdEntity();
                                e2 = new VersioningJpaLongIdEntity();

                                // ensure the other instantiate fragment is valid
                                otherRepo.save(new VersioningJpaSiblingEntity());

                                e3 = new VersioningJpaLongIdEntity();

                                setupSecurityContext("some-principal", true);

                                try {
                                    result = repo.save(e3);
                                } catch (Exception e) {
                                    JpaLockingAndVersioningRepositoryImplIT.this.e = e;
                                }
                            }

                            @Test
                            void shouldMergeAndReturnTheEntity() {
                                assertThat(e).isNull();
                                assertThat(result.getXid()).isEqualTo(e3.getXid());
                            }
                        }
                    }

                    @Nested

                    @DisplayName("Given the entity exists")

                    class EntityExists {
                        @Nested
                        @DisplayName("Given the principal is the lock owner")
                        class PrincipalIsTheLockOwner {
                            @BeforeEach
                            void setUp() {
                                repo = context.getBean(VersioningJpaLongIdRepository.class);

                                e1 = new VersioningJpaLongIdEntity();
                                e2 = new VersioningJpaLongIdEntity();

                                // ensure the other instantiate fragment is valid
                                otherRepo.save(new VersioningJpaSiblingEntity());

                                e3 = new VersioningJpaLongIdEntity();

                                setupSecurityContext("some-principal", true);

                                e3 = repo.save(e3);

                                e3 = repo.lock(e3);

                                try {
                                    result = repo.save(e3);
                                } catch (Exception e) {
                                    JpaLockingAndVersioningRepositoryImplIT.this.e = e;
                                }
                            }

                            @Test
                            void shouldReturnTheEntity() {
                                assertThat(e).isNull();
                                assertThat(result.getXid()).isEqualTo(e3.getXid());
                            }
                        }

                        @Nested

                        @DisplayName("Given the principal is not the lock owner")

                        class PrincipalIsNotTheLockOwner {
                            @BeforeEach
                            void setUp() {
                                repo = context.getBean(VersioningJpaLongIdRepository.class);

                                e1 = new VersioningJpaLongIdEntity();
                                e2 = new VersioningJpaLongIdEntity();

                                // ensure the other instantiate fragment is valid
                                otherRepo.save(new VersioningJpaSiblingEntity());

                                e3 = new VersioningJpaLongIdEntity();

                                setupSecurityContext("some-principal", true);

                                e3 = repo.save(e3);

                                e3 = repo.lock(e3);
                                setupSecurityContext("some-other-principal", true);

                                try {
                                    result = repo.save(e3);
                                } catch (Exception e) {
                                    JpaLockingAndVersioningRepositoryImplIT.this.e = e;
                                }
                            }

                            @Test
                            void shouldThrowALockOwnerException() {
                                assertThat(e).isInstanceOf(LockOwnerException.class);
                            }
                        }

                        @Nested

                        @DisplayName("Given there is no lock owner")

                        class ThereIsNoLockOwner {
                            @BeforeEach
                            void setUp() {
                                repo = context.getBean(VersioningJpaLongIdRepository.class);

                                e1 = new VersioningJpaLongIdEntity();
                                e2 = new VersioningJpaLongIdEntity();

                                // ensure the other instantiate fragment is valid
                                otherRepo.save(new VersioningJpaSiblingEntity());

                                e3 = new VersioningJpaLongIdEntity();

                                setupSecurityContext("some-principal", true);

                                e3 = repo.save(e3);

                                try {
                                    result = repo.save(e3);
                                } catch (Exception e) {
                                    JpaLockingAndVersioningRepositoryImplIT.this.e = e;
                                }
                            }

                            @Test
                            void shouldMergeAndReturnTheEntity() {
                                assertThat(e).isNull();
                                assertThat(result.getXid()).isEqualTo(e3.getXid());
                            }
                        }
                    }
                }

                @Nested

                @DisplayName("Given an unauthenticated principal")

                class UnauthenticatedPrincipal {
                    @Nested
                    @DisplayName("Given the entity is new")
                    class EntityIsNew {
                        @Nested
                        @DisplayName("Given there is no lock owner")
                        class ThereIsNoLockOwner {
                            @BeforeEach
                            void setUp() {
                                repo = context.getBean(VersioningJpaLongIdRepository.class);

                                e1 = new VersioningJpaLongIdEntity();
                                e2 = new VersioningJpaLongIdEntity();

                                // ensure the other instantiate fragment is valid
                                otherRepo.save(new VersioningJpaSiblingEntity());

                                e3 = new VersioningJpaLongIdEntity();

                                setupSecurityContext("some-principal", false);

                                try {
                                    result = repo.save(e3);
                                } catch (Exception e) {
                                    JpaLockingAndVersioningRepositoryImplIT.this.e = e;
                                }
                            }

                            @Test
                            void shouldMergeAndReturnTheEntity() {
                                assertThat(e).isNull();
                                assertThat(result.getXid()).isEqualTo(e3.getXid());
                            }
                        }
                    }

                    @Nested

                    @DisplayName("Given the entity exists")

                    class EntityExists {
                        @Nested
                        @DisplayName("Given there is no lock owner")
                        class ThereIsNoLockOwner {
                            @BeforeEach
                            void setUp() {
                                repo = context.getBean(VersioningJpaLongIdRepository.class);

                                e1 = new VersioningJpaLongIdEntity();
                                e2 = new VersioningJpaLongIdEntity();

                                // ensure the other instantiate fragment is valid
                                otherRepo.save(new VersioningJpaSiblingEntity());

                                e3 = new VersioningJpaLongIdEntity();

                                setupSecurityContext("some-principal", false);

                                e3 = repo.save(e3);

                                try {
                                    result = repo.save(e3);
                                } catch (Exception e) {
                                    JpaLockingAndVersioningRepositoryImplIT.this.e = e;
                                }
                            }

                            @Test
                            void shouldMergeAndReturnTheEntity() {
                                assertThat(e).isNull();
                                assertThat(result.getXid()).isEqualTo(e3.getXid());
                            }
                        }
                    }
                }

                @Nested

                @DisplayName("Given there is no principal")

                class ThereIsNoPrincipal {
                    @Nested
                    @DisplayName("Given the entity is new")
                    class EntityIsNew {
                        @Nested
                        @DisplayName("Given there is no lock owner")
                        class ThereIsNoLockOwner {
                            @BeforeEach
                            void setUp() {
                                repo = context.getBean(VersioningJpaLongIdRepository.class);

                                e1 = new VersioningJpaLongIdEntity();
                                e2 = new VersioningJpaLongIdEntity();

                                // ensure the other instantiate fragment is valid
                                otherRepo.save(new VersioningJpaSiblingEntity());

                                e3 = new VersioningJpaLongIdEntity();

                                try {
                                    result = repo.save(e3);
                                } catch (Exception e) {
                                    JpaLockingAndVersioningRepositoryImplIT.this.e = e;
                                }
                            }

                            @Test
                            void shouldSucceed() {
                                assertThat(e).isNull();
                                assertThat(result.getXid()).isEqualTo(e3.getXid());
                            }
                        }
                    }
                }
            }

            @Nested
           class FindAllVersions {
                @BeforeEach
                void setUp() {
                    repo = context.getBean(VersioningJpaLongIdRepository.class);

                    e1 = new VersioningJpaLongIdEntity();
                    e2 = new VersioningJpaLongIdEntity();

                    // ensure the other instantiate fragment is valid
                    otherRepo.save(new VersioningJpaSiblingEntity());

                    setupSecurityContext("some-principal", true);

                    e1 = repo.save(e1);
                    e1 = repo.lock(e1);
                    e1v11 = repo.version(e1, new VersionInfo("1.1", "Minor"));
                    e1v11 = repo.unlock(e1v11);

                    e2 = repo.save(e2);
                    e2 = repo.lock(e2);
                    e2v2 = repo.version(e2, new VersionInfo("2.0", "Major"));
                    e2v2 = repo.unlock(e2v2);
                }

                @Test
                void shouldReturnTheVersionSeries() {
                    List<VersioningJpaLongIdEntity> results = repo.findAllVersions(e1, Sort.by(Order.desc("id")));
                    assertThat(results.size()).isEqualTo(2);
                    assertThat(results).extracting("xid").contains(e1.getXid(), e1v11.getXid());
                }

                @Test
                void shouldReturnTheOrderedVersionSeries() {
                    List<VersioningJpaLongIdEntity> results = repo.findAllVersions(e1, Sort.by(Order.desc("id")));
                    assertThat(results.size()).isEqualTo(2);
                    assertThat(results).extracting("xid").containsExactly(e1v11.getXid(), e1.getXid());
                }
            }

            @Nested
           class FindAllLatestVersions {
                @BeforeEach
                void setUp() {
                    repo = context.getBean(VersioningJpaLongIdRepository.class);

                    e1 = new VersioningJpaLongIdEntity();
                    e2 = new VersioningJpaLongIdEntity();

                    // ensure the other instantiate fragment is valid
                    otherRepo.save(new VersioningJpaSiblingEntity());

                    setupSecurityContext("some-principal", true);

                    e1 = repo.save(e1);
                    e1 = repo.lock(e1);
                    e1v11 = repo.version(e1, new VersionInfo("1.1", "Minor"));
                    e1v11 = repo.unlock(e1v11);

                    e2 = repo.save(e2);
                    e2 = repo.lock(e2);
                    e2v2 = repo.version(e2, new VersionInfo("2.0", "Major"));
                    e2v2 = repo.unlock(e2v2);

                    e2v2 = repo.lock(e2v2);
                    e3wc = repo.workingCopy(e2v2);
                }

                @Test
                void shouldReturnOnlyTheLatestVersionOfTheEntities() {
                    List<VersioningJpaLongIdEntity> results = repo.findAllVersionsLatest(VersioningJpaLongIdEntity.class);
                    assertThat(results).extracting("xid").contains(e1v11.getXid(), e2v2.getXid()).doesNotContain(e3wc.getXid());
                }
            }

            @Nested
           class WorkingCopy {
                @Nested
                @DisplayName("When the entity is new")
                class EntityIsNew {
                    @BeforeEach
                    void setUp() {
                        repo = context.getBean(VersioningJpaLongIdRepository.class);

                        e1 = new VersioningJpaLongIdEntity();
                        e2 = new VersioningJpaLongIdEntity();

                        // ensure the other instantiate fragment is valid
                        otherRepo.save(new VersioningJpaSiblingEntity());

                        setupSecurityContext("some-principal", true);

                        try {
                            result = repo.workingCopy(e1);
                        } catch (Exception e) {
                            JpaLockingAndVersioningRepositoryImplIT.this.e = e;
                        }
                    }

                    @Test
                    void shouldFail() {
                        assertThat(e).isInstanceOf(InvalidDataAccessApiUsageException.class);
                    }
                }

                @Nested

                @DisplayName("When the entity exists")

                class EntityExists {
                    @Nested
                    @DisplayName("Given the principal is the lock owner")
                    class PrincipalIsTheLockOwner {
                        @Nested
                        @DisplayName("When the entity is not yet part of a version tree")
                        class EntityIsNotYetPartOfAVersionTree {
                            @BeforeEach
                            void setUp() {
                                repo = context.getBean(VersioningJpaLongIdRepository.class);

                                e1 = new VersioningJpaLongIdEntity();
                                e2 = new VersioningJpaLongIdEntity();

                                // ensure the other instantiate fragment is valid
                                otherRepo.save(new VersioningJpaSiblingEntity());

                                setupSecurityContext("some-principal", true);

                                e1 = repo.save(e1);

                                setupSecurityContext("some-principal", true);
                                e1 = repo.lock(e1);

                                try {
                                    result = repo.workingCopy(e1);
                                } catch (Exception e) {
                                    JpaLockingAndVersioningRepositoryImplIT.this.e = e;
                                }
                            }

                            @Test
                            void shouldCreateThePwcWithANewId() {
                                assertThat(result.getXid()).isNotNull();
                                assertThat(result.getVersionLabel()).isEqualTo("~~PWC~~");
                                assertThat(result.getXAncestorId()).isEqualTo(e1.getXid());
                                assertThat(result.getXAncestorRootId()).isEqualTo(e1.getXid());
                                assertThat(result.getXSuccessorId()).isNull();
                            }
                        }
                    }

                    @Nested

                    @DisplayName("Given the principal is not the lock owner")

                    class PrincipalIsNotTheLockOwner {
                        @BeforeEach
                        void setUp() {
                            repo = context.getBean(VersioningJpaLongIdRepository.class);

                            e1 = new VersioningJpaLongIdEntity();
                            e2 = new VersioningJpaLongIdEntity();

                            // ensure the other instantiate fragment is valid
                            otherRepo.save(new VersioningJpaSiblingEntity());

                            setupSecurityContext("some-principal", true);

                            e1 = repo.save(e1);

                            setupSecurityContext("some-principal", true);
                            e1 = repo.lock(e1);
                            setupSecurityContext("some-other-principal", true);

                            try {
                                result = repo.workingCopy(e1);
                            } catch (Exception e) {
                                JpaLockingAndVersioningRepositoryImplIT.this.e = e;
                            }
                        }

                        @Test
                        void shouldCreateThePwcWithANewId() {
                            assertThat(e).isInstanceOf(LockOwnerException.class);
                        }
                    }

                    @Nested

                    @DisplayName("Given the principal is unauthenticated")

                    class PrincipalIsUnauthenticated {
                        @BeforeEach
                        void setUp() {
                            repo = context.getBean(VersioningJpaLongIdRepository.class);

                            e1 = new VersioningJpaLongIdEntity();
                            e2 = new VersioningJpaLongIdEntity();

                            // ensure the other instantiate fragment is valid
                            otherRepo.save(new VersioningJpaSiblingEntity());

                            setupSecurityContext("some-principal", true);

                            e1 = repo.save(e1);

                            setupSecurityContext("some-principal", false);

                            try {
                                result = repo.workingCopy(e1);
                            } catch (Exception e) {
                                JpaLockingAndVersioningRepositoryImplIT.this.e = e;
                            }
                        }

                        @Test
                        void shouldThrowASecurityException() {
                            assertThat(e).isInstanceOf(SecurityException.class);
                            assertThat(e.getMessage()).contains("no principal");
                        }
                    }

                    @Nested

                    @DisplayName("Given the entity is not the current version")

                    class EntityIsNotTheCurrentVersion {
                        @BeforeEach
                        void setUp() {
                            repo = context.getBean(VersioningJpaLongIdRepository.class);

                            e1 = new VersioningJpaLongIdEntity();
                            e2 = new VersioningJpaLongIdEntity();

                            // ensure the other instantiate fragment is valid
                            otherRepo.save(new VersioningJpaSiblingEntity());

                            setupSecurityContext("some-principal", true);

                            e1 = repo.save(e1);

                            setupSecurityContext("some-principal", true);
                            e1 = repo.lock(e1);
                            e1v11 = repo.version(e1, new VersionInfo("1.1", "Minor"));

                            try {
                                result = repo.workingCopy(e1);
                            } catch (Exception e) {
                                JpaLockingAndVersioningRepositoryImplIT.this.e = e;
                            }
                        }

                        @Test
                        void shouldThrowAnException() {
                            assertThat(e).isInstanceOf(LockingAndVersioningException.class);
                            assertThat(e.getMessage()).contains("not head");
                        }
                    }
                }
            }

            @Nested
           class IsPrivateWorkingCopy {
                @BeforeEach
                void setUp() {
                    repo = context.getBean(VersioningJpaLongIdRepository.class);

                    e1 = new VersioningJpaLongIdEntity();
                    e2 = new VersioningJpaLongIdEntity();

                    // ensure the other instantiate fragment is valid
                    otherRepo.save(new VersioningJpaSiblingEntity());

                    setupSecurityContext("some-principal", true);
                }

                @Test
                void shouldReturnFalse() {
                    assertThat(repo.isPrivateWorkingCopy(e1)).isFalse();
                }

                @Test
                void shouldReturnTrue() {
                    e1 = repo.save(e1);
                    e1 = repo.lock(e1);
                    VersioningJpaLongIdEntity wc = repo.workingCopy(e1);
                    assertThat(repo.isPrivateWorkingCopy(wc)).isTrue();
                }
            }

            @Nested
           class FindWorkingCopy {
                @BeforeEach
                void setUp() {
                    repo = context.getBean(VersioningJpaLongIdRepository.class);

                    e1 = new VersioningJpaLongIdEntity();
                    e2 = new VersioningJpaLongIdEntity();

                    // ensure the other instantiate fragment is valid
                    otherRepo.save(new VersioningJpaSiblingEntity());

                    setupSecurityContext("some-principal", true);
                }

                @Test
                void shouldReturnTrue() {
                    e1 = repo.save(e1);
                    e1 = repo.lock(e1);
                    VersioningJpaLongIdEntity wc = repo.workingCopy(e1);
                    assertThat(repo.findWorkingCopy(wc)).hasFieldOrPropertyWithValue("xid", wc.getXid());
                }
            }

            @Nested
           class Delete {
                @Nested
                @DisplayName("Given no principal")
                class NoPrincipal {
                    @BeforeEach
                    void setUp() {
                        repo = context.getBean(VersioningJpaLongIdRepository.class);

                        e1 = new VersioningJpaLongIdEntity();
                        e2 = new VersioningJpaLongIdEntity();

                        // ensure the other instantiate fragment is valid
                        otherRepo.save(new VersioningJpaSiblingEntity());

                        setupSecurityContext(null, false);
                        entityForDeletion = e1;

                        try {
                            repo.delete(entityForDeletion);
                        } catch (Exception e) {
                            JpaLockingAndVersioningRepositoryImplIT.this.e = e;
                        }
                    }

                    @Test
                    void shouldThrowASecurityException() {
                        assertThat(e).isInstanceOf(SecurityException.class);
                    }
                }

                @Nested

                @DisplayName("Given an unauthenticated principal")

                class UnauthenticatedPrincipal {
                    @BeforeEach
                    void setUp() {
                        repo = context.getBean(VersioningJpaLongIdRepository.class);

                        e1 = new VersioningJpaLongIdEntity();
                        e2 = new VersioningJpaLongIdEntity();

                        // ensure the other instantiate fragment is valid
                        otherRepo.save(new VersioningJpaSiblingEntity());

                        setupSecurityContext("some-principal", false);
                        entityForDeletion = e1;

                        try {
                            repo.delete(entityForDeletion);
                        } catch (Exception e) {
                            JpaLockingAndVersioningRepositoryImplIT.this.e = e;
                        }
                    }

                    @Test
                    void shouldThrowASecurityException() {
                        assertThat(e).isInstanceOf(SecurityException.class);
                    }
                }

                @Nested

                @DisplayName("Given a principal")

                class Principal {
                    @Nested
                    @DisplayName("Given the entity is not in a version tree")
                    class EntityIsNotInAVersionTree {
                        @Nested
                        @DisplayName("Given the principal is the lock owner")
                        class PrincipalIsTheLockOwner {
                            @BeforeEach
                            void setUp() {
                                repo = context.getBean(VersioningJpaLongIdRepository.class);

                                e1 = new VersioningJpaLongIdEntity();
                                e2 = new VersioningJpaLongIdEntity();

                                // ensure the other instantiate fragment is valid
                                otherRepo.save(new VersioningJpaSiblingEntity());

                                setupSecurityContext("some-principal", true);

                                e1 = repo.save(e1);
                                e1 = repo.lock(e1);
                                entityForDeletion = e1;

                                try {
                                    repo.delete(entityForDeletion);
                                } catch (Exception e) {
                                    JpaLockingAndVersioningRepositoryImplIT.this.e = e;
                                }
                            }

                            @Test
                            void shouldBeDeleted() {
                                assertThat(e).isNull();
                                assertThat(repo.findById(e1.getXid())).isEqualTo(Optional.empty());
                            }
                        }

                        @Nested

                        @DisplayName("Given the principal is not the lock owner")

                        class PrincipalIsNotTheLockOwner {
                            @BeforeEach
                            void setUp() {
                                repo = context.getBean(VersioningJpaLongIdRepository.class);

                                e1 = new VersioningJpaLongIdEntity();
                                e2 = new VersioningJpaLongIdEntity();

                                // ensure the other instantiate fragment is valid
                                otherRepo.save(new VersioningJpaSiblingEntity());

                                setupSecurityContext("some-principal", true);

                                e1 = repo.save(e1);
                                e1 = repo.lock(e1);
                                entityForDeletion = e1;
                                setupSecurityContext("some-other-principal", true);

                                try {
                                    repo.delete(entityForDeletion);
                                } catch (Exception e) {
                                    JpaLockingAndVersioningRepositoryImplIT.this.e = e;
                                }
                            }

                            @Test
                            void shouldFailToDeleteTheEntity() {
                                assertThat(e).isInstanceOf(LockOwnerException.class);
                                assertThat(e.getMessage()).contains("not lock owner");
                            }
                        }

                        @Nested

                        @DisplayName("Given there is no lock")

                        class ThereIsNoLock {
                            @BeforeEach
                            void setUp() {
                                repo = context.getBean(VersioningJpaLongIdRepository.class);

                                e1 = new VersioningJpaLongIdEntity();
                                e2 = new VersioningJpaLongIdEntity();

                                // ensure the other instantiate fragment is valid
                                otherRepo.save(new VersioningJpaSiblingEntity());

                                setupSecurityContext("some-principal", true);

                                e1 = repo.save(e1);
                                entityForDeletion = e1;

                                try {
                                    repo.delete(entityForDeletion);
                                } catch (Exception e) {
                                    JpaLockingAndVersioningRepositoryImplIT.this.e = e;
                                }
                            }

                            @Test
                            void shouldBeDeleted() {
                                assertThat(e).isNull();
                                assertThat(repo.findById(e1.getXid())).isEqualTo(Optional.empty());
                            }
                        }
                    }

                    @Nested

                    @DisplayName("Given the entity is not the HEAD")

                    class EntityIsNotTheHead {
                        @BeforeEach
                        void setUp() {
                            repo = context.getBean(VersioningJpaLongIdRepository.class);

                            e1 = new VersioningJpaLongIdEntity();
                            e2 = new VersioningJpaLongIdEntity();

                            // ensure the other instantiate fragment is valid
                            otherRepo.save(new VersioningJpaSiblingEntity());

                            setupSecurityContext("some-principal", true);

                            e1 = repo.save(e1);
                            e1 = repo.lock(e1);
                            e1v11 = repo.version(e1, new VersionInfo("1.1", "Minor"));
                            entityForDeletion = e1;

                            try {
                                repo.delete(entityForDeletion);
                            } catch (Exception e) {
                                JpaLockingAndVersioningRepositoryImplIT.this.e = e;
                            }
                        }

                        @Test
                        void shouldFail() {
                            assertThat(e).isInstanceOf(LockingAndVersioningException.class);
                            assertThat(e.getMessage()).contains("not head");
                        }
                    }

                    @Nested

                    @DisplayName("Given the entity is the HEAD of a version series of3versions and the ancestor is not ancestral ro")

                    class EntityIsTheHeadOfAVersionSeriesOf3VersionsAndTheAncestorIsNotAncestralRo {
                        @BeforeEach
                        void setUp() {
                            repo = context.getBean(VersioningJpaLongIdRepository.class);

                            e1 = new VersioningJpaLongIdEntity();
                            e2 = new VersioningJpaLongIdEntity();

                            // ensure the other instantiate fragment is valid
                            otherRepo.save(new VersioningJpaSiblingEntity());

                            setupSecurityContext("some-principal", true);

                            e1 = repo.save(e1);
                            e1 = repo.lock(e1);
                            e1v11 = repo.version(e1, new VersionInfo("1.1", "Minor"));
                            e1v12 = repo.version(e1v11, new VersionInfo("1.2", "Minor"));
                            entityForDeletion = e1v12;

                            try {
                                repo.delete(entityForDeletion);
                            } catch (Exception e) {
                                JpaLockingAndVersioningRepositoryImplIT.this.e = e;
                            }
                        }

                        @Test
                        void shouldDeleteTheEntity() {
                            assertThat(repo.findById(e1v12.getXid())).isEqualTo(Optional.empty());
                        }

                        @Test
                        void shouldReInstateTheAncestorAsTheHead() {
                            e1v11 = repo.findById(e1v11.getXid()).orElseThrow();
                            assertThat(e1v11.getXSuccessorId()).isNull();
                        }

                        @Test
                        void shouldRemoveTheLock() {
                            assertThat(lockingService.lockOwner(e1v12.getXid())).isNull();
                        }

                        @Test
                        void shouldReInstateTheLockOnTheNewHead() {
                            assertThat(lockingService.lockOwner(e1v11.getXid())).isNotNull();
                        }
                    }

                    @Nested

                    @DisplayName("Given the entity is the HEAD of a version tree of2versions ancestor is ancestral root")

                    class EntityIsTheHeadOfAVersionTreeOf2VersionsAncestorIsAncestralRoot {
                        @BeforeEach
                        void setUp() {
                            repo = context.getBean(VersioningJpaLongIdRepository.class);

                            e1 = new VersioningJpaLongIdEntity();
                            e2 = new VersioningJpaLongIdEntity();

                            // ensure the other instantiate fragment is valid
                            otherRepo.save(new VersioningJpaSiblingEntity());

                            setupSecurityContext("some-principal", true);

                            e1 = repo.save(e1);
                            e1 = repo.lock(e1);
                            e1v11 = repo.version(e1, new VersionInfo("1.1", "Minor"));
                            entityForDeletion = e1v11;

                            try {
                                repo.delete(entityForDeletion);
                            } catch (Exception e) {
                                JpaLockingAndVersioningRepositoryImplIT.this.e = e;
                            }
                        }

                        @Test
                        void shouldDeleteTheEntity() {
                            assertThat(e).isNull();
                            assertThat(repo.findById(e1v11.getXid())).isEqualTo(Optional.empty());
                        }

                        @Test
                        void shouldReInstateTheAncestorAsTheHead() {
                            e1 = repo.findById(e1.getXid()).orElseThrow();
                            assertThat(e1.getXSuccessorId()).isNull();
                        }
                    }
                }
            }

            @Nested
           class DeleteAllVersions {
                @Nested
                @DisplayName("Given no principal")
                class NoPrincipal {
                    @BeforeEach
                    void setUp() {
                        repo = context.getBean(VersioningJpaLongIdRepository.class);

                        e1 = new VersioningJpaLongIdEntity();
                        e2 = new VersioningJpaLongIdEntity();

                        // ensure the other instantiate fragment is valid
                        otherRepo.save(new VersioningJpaSiblingEntity());

                        setupSecurityContext("some-principal", true);

                        e1 = repo.save(e1);
                        e1 = repo.lock(e1);
                        e1v11 = repo.version(e1, new VersionInfo("1.1", "Minor"));
                        e1v11 = repo.unlock(e1v11);

                        setupSecurityContext(null, false);
                    }

                    @Test
                    void shouldThrowASecurityException() {
                        assertThatThrownBy(() -> repo.deleteAllVersions(e1v11))
                            .isInstanceOf(SecurityException.class);
                    }
                }

                @Nested

                @DisplayName("Given an unauthenticated principal")

                class UnauthenticatedPrincipal {
                    @BeforeEach
                    void setUp() {
                        repo = context.getBean(VersioningJpaLongIdRepository.class);

                        e1 = new VersioningJpaLongIdEntity();
                        e2 = new VersioningJpaLongIdEntity();

                        // ensure the other instantiate fragment is valid
                        otherRepo.save(new VersioningJpaSiblingEntity());

                        setupSecurityContext("some-principal", true);

                        e1 = repo.save(e1);
                        e1 = repo.lock(e1);
                        e1v11 = repo.version(e1, new VersionInfo("1.1", "Minor"));
                        e1v11 = repo.unlock(e1v11);

                        setupSecurityContext("some-principal", false);
                    }

                    @Test
                    void shouldThrowASecurityException() {
                        assertThatThrownBy(() -> repo.deleteAllVersions(e1v11))
                            .isInstanceOf(SecurityException.class);
                    }
                }

                @Nested

                @DisplayName("Given a principal")

                class Principal {
                    @Nested
                    @DisplayName("Given the entity is not in a version tree")
                    class EntityIsNotInAVersionTree {
                        @Nested
                        @DisplayName("Given the principal is the lock owner")
                        class PrincipalIsTheLockOwner {
                            @BeforeEach
                            void setUp() {
                                repo = context.getBean(VersioningJpaLongIdRepository.class);

                                e1 = new VersioningJpaLongIdEntity();
                                e2 = new VersioningJpaLongIdEntity();

                                // ensure the other instantiate fragment is valid
                                otherRepo.save(new VersioningJpaSiblingEntity());

                                setupSecurityContext("some-principal", true);

                                e1 = repo.save(e1);
                                e1 = repo.lock(e1);
                                e1v11 = repo.version(e1, new VersionInfo("1.1", "Minor"));
                                e1v11 = repo.unlock(e1v11);

                                setupSecurityContext("some-principal", true);
                            }

                            @Test
                            void shouldDeleteVersionSeries() {
                                e1v11 = repo.lock(e1v11);

                                List<Long> ids = new ArrayList<>();
                                repo.findAllVersions(e1v11).forEach((doc) -> ids.add(doc.getXid()));

                                repo.deleteAllVersions(e1v11);

                                ids.forEach((id) -> assertThat(repo.existsById(id)).isFalse());
                            }
                        }

                        @Nested

                        @DisplayName("Given the principal is not the lock owner")

                        class PrincipalIsNotTheLockOwner {
                            @BeforeEach
                            void setUp() {
                                repo = context.getBean(VersioningJpaLongIdRepository.class);

                                e1 = new VersioningJpaLongIdEntity();
                                e2 = new VersioningJpaLongIdEntity();

                                // ensure the other instantiate fragment is valid
                                otherRepo.save(new VersioningJpaSiblingEntity());

                                setupSecurityContext("some-principal", true);

                                e1 = repo.save(e1);
                                e1 = repo.lock(e1);
                                e1v11 = repo.version(e1, new VersionInfo("1.1", "Minor"));
                                e1v11 = repo.unlock(e1v11);

                                setupSecurityContext("some-principal", true);

                                e1v11 = repo.lock(e1v11);
                                setupSecurityContext("some-other-principal", true);
                            }

                            @Test
                            void shouldFailToDeleteTheEntity() {
                                assertThatThrownBy(() -> repo.deleteAllVersions(e1v11))
                                    .isInstanceOf(LockOwnerException.class)
                                    .hasMessageContaining("not lock owner");
                            }
                        }

                        @Nested

                        @DisplayName("Given there is no lock")

                        class ThereIsNoLock {
                            @BeforeEach
                            void setUp() {
                                repo = context.getBean(VersioningJpaLongIdRepository.class);

                                e1 = new VersioningJpaLongIdEntity();
                                e2 = new VersioningJpaLongIdEntity();

                                // ensure the other instantiate fragment is valid
                                otherRepo.save(new VersioningJpaSiblingEntity());

                                setupSecurityContext("some-principal", true);

                                e1 = repo.save(e1);
                                e1 = repo.lock(e1);
                                e1v11 = repo.version(e1, new VersionInfo("1.1", "Minor"));
                                e1v11 = repo.unlock(e1v11);

                                setupSecurityContext("some-principal", true);
                            }

                            @Test
                            void shouldDeleteVersionSeries() {
                                List<Long> ids = new ArrayList<>();
                                repo.findAllVersions(e1).forEach((doc) -> ids.add(doc.getXid()));

                                repo.deleteAllVersions(e1v11);

                                ids.forEach((id) -> assertThat(repo.existsById(id)).isFalse());
                            }
                        }
                    }

                    @Nested

                    @DisplayName("Given the entity is not the HEAD")

                    class EntityIsNotTheHead {
                        @BeforeEach
                        void setUp() {
                            repo = context.getBean(VersioningJpaLongIdRepository.class);

                            e1 = new VersioningJpaLongIdEntity();
                            e2 = new VersioningJpaLongIdEntity();

                            // ensure the other instantiate fragment is valid
                            otherRepo.save(new VersioningJpaSiblingEntity());

                            setupSecurityContext("some-principal", true);

                            e1 = repo.save(e1);
                            e1 = repo.lock(e1);
                            e1v11 = repo.version(e1, new VersionInfo("1.1", "Minor"));
                            e1v11 = repo.unlock(e1v11);

                            setupSecurityContext("some-principal", true);
                        }

                        @Test
                        void shouldFail() {
                            assertThatThrownBy(() -> repo.deleteAllVersions(e1))
                                .isInstanceOf(LockingAndVersioningException.class)
                                .hasMessageContaining("not head");
                        }
                    }
                }
            }

            @Nested
           class Issue2039 {
                @BeforeEach
                void setUp() {
                    repo = context.getBean(VersioningJpaLongIdRepository.class);

                    e1 = new VersioningJpaLongIdEntity();
                    e2 = new VersioningJpaLongIdEntity();

                    // ensure the other instantiate fragment is valid
                    otherRepo.save(new VersioningJpaSiblingEntity());

                    setupSecurityContext("some-principal", true);

                    e1 = repo.save(new VersioningJpaLongIdEntity());
                    e2 = repo.save(new VersioningJpaLongIdEntity());
                    e3 = repo.save(new VersioningJpaLongIdEntity());
                }

                @Test
                void shouldReturnTheProvidedEntity() {
                    List<VersioningJpaLongIdEntity> results = repo.findAllVersions(e1, Sort.by(Order.desc("id")));
                    assertThat(results.size()).isEqualTo(1);
                }

                @Test
                void shouldDeleteJustTheProvidedEntity() {
                    repo.deleteAllVersions(e1);

                    Optional<VersioningJpaLongIdEntity> fetched = repo.findById(e1.getXid());
                    assertThat(fetched.isPresent()).isFalse();
                }
            }
        }
    }
}
