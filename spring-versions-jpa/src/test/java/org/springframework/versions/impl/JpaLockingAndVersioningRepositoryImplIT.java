package org.springframework.versions.impl;

import org.junit.jupiter.api.extension.ExtendWith;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.versions.AncestorId;
import org.springframework.versions.AncestorRootId;
import org.springframework.versions.LockOwner;
import org.springframework.versions.LockOwnerException;
import org.springframework.versions.LockingAndVersioningException;
import org.springframework.versions.LockingAndVersioningRepository;
import org.springframework.versions.SuccessorId;
import org.springframework.versions.VersionInfo;
import org.springframework.versions.VersionLabel;
import org.springframework.versions.VersionNumber;
import org.springframework.versions.jpa.config.JpaLockingAndVersioningConfig;

import internal.org.springframework.versions.LockingService;

@ContextConfiguration(classes={JpaLockingAndVersioningRepositoryImplIT.TestConfig.class})
@ExtendWith(SpringExtension.class)
public class JpaLockingAndVersioningRepositoryImplIT {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private LockingService lockingService;

    private TestRepository repo;

    private TestEntity e1, e2, e3, e1v11, e1v12, e2v2, e3wc, entityForDeletion, result;

    private Exception e;

    //////////////////////////////////////////////////
    // this entity and repository ensure that multiple repositories can implement LockingAndVersioningRepository at the same time
    @Autowired
    private OtherTestRepository otherRepo;

    @Nested
    class GivenALockingAndVersioningRepositoryAndASecurityContext {
        @Nested
        class GivenTwoEntitiesWithTwoVersionsEach {
            @Nested
            class Lock {
                @Nested
                class GivenANullPrincipal {
                    @BeforeEach
                    void setUp() {
                        repo = context.getBean(TestRepository.class);

                        e1 = new TestEntity();
                        e2 = new TestEntity();

                        // ensure the other instantiate fragment is valid
                        OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

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
                class GivenTheEntityIsNew {
                    @BeforeEach
                    void setUp() {
                        repo = context.getBean(TestRepository.class);

                        e1 = new TestEntity();
                        e2 = new TestEntity();

                        // ensure the other instantiate fragment is valid
                        OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

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
                class GivenTheEntityExists {
                    @Nested
                    class WhenTheObjectIsNotLocked {
                        @BeforeEach
                        void setUp() {
                            repo = context.getBean(TestRepository.class);

                            e1 = new TestEntity();
                            e2 = new TestEntity();

                            // ensure the other instantiate fragment is valid
                            OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

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
                    class WhenTheLockIsAlreadyTaken {
                        @BeforeEach
                        void setUp() {
                            repo = context.getBean(TestRepository.class);

                            e1 = new TestEntity();
                            e2 = new TestEntity();

                            // ensure the other instantiate fragment is valid
                            OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

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
                    class WhenTheLockIsAlreadyHeld {
                        @BeforeEach
                        void setUp() {
                            repo = context.getBean(TestRepository.class);

                            e1 = new TestEntity();
                            e2 = new TestEntity();

                            // ensure the other instantiate fragment is valid
                            OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

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
                    class WhenThePrincipalIsNotAuthenticated {
                        @BeforeEach
                        void setUp() {
                            repo = context.getBean(TestRepository.class);

                            e1 = new TestEntity();
                            e2 = new TestEntity();

                            // ensure the other instantiate fragment is valid
                            OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

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
                class GivenANullPrincipal {
                    @BeforeEach
                    void setUp() {
                        repo = context.getBean(TestRepository.class);

                        e1 = new TestEntity();
                        e2 = new TestEntity();

                        // ensure the other instantiate fragment is valid
                        OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

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
                class GivenTheEntityIsNew {
                    @BeforeEach
                    void setUp() {
                        repo = context.getBean(TestRepository.class);

                        e1 = new TestEntity();
                        e2 = new TestEntity();

                        // ensure the other instantiate fragment is valid
                        OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

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
                class GivenTheEntityExists {
                    @Nested
                    class GivenThePrincipalIsTheLockOwner {
                        @BeforeEach
                        void setUp() {
                            repo = context.getBean(TestRepository.class);

                            e1 = new TestEntity();
                            e2 = new TestEntity();

                            // ensure the other instantiate fragment is valid
                            OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

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
                    class GivenThePrincipalIsNotTheLockOwner {
                        @BeforeEach
                        void setUp() {
                            repo = context.getBean(TestRepository.class);

                            e1 = new TestEntity();
                            e2 = new TestEntity();

                            // ensure the other instantiate fragment is valid
                            OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

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
                    class GivenThePrincipalIsNotAuthenticated {
                        @BeforeEach
                        void setUp() {
                            repo = context.getBean(TestRepository.class);

                            e1 = new TestEntity();
                            e2 = new TestEntity();

                            // ensure the other instantiate fragment is valid
                            OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

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
                class GivenAnAuthenticatedPrincipal {
                    @Nested
                    class GivenTheEntityIsNew {
                        @Nested
                        class GivenThereIsNoLockOwner {
                            @BeforeEach
                            void setUp() {
                                repo = context.getBean(TestRepository.class);

                                e1 = new TestEntity();
                                e2 = new TestEntity();

                                // ensure the other instantiate fragment is valid
                                OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

                                e3 = new TestEntity();

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
                    class GivenTheEntityExists {
                        @Nested
                        class GivenThePrincipalIsTheLockOwner {
                            @BeforeEach
                            void setUp() {
                                repo = context.getBean(TestRepository.class);

                                e1 = new TestEntity();
                                e2 = new TestEntity();

                                // ensure the other instantiate fragment is valid
                                OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

                                e3 = new TestEntity();

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
                        class GivenThePrincipalIsNotTheLockOwner {
                            @BeforeEach
                            void setUp() {
                                repo = context.getBean(TestRepository.class);

                                e1 = new TestEntity();
                                e2 = new TestEntity();

                                // ensure the other instantiate fragment is valid
                                OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

                                e3 = new TestEntity();

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
                        class GivenThereIsNoLockOwner {
                            @BeforeEach
                            void setUp() {
                                repo = context.getBean(TestRepository.class);

                                e1 = new TestEntity();
                                e2 = new TestEntity();

                                // ensure the other instantiate fragment is valid
                                OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

                                e3 = new TestEntity();

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
                class GivenAnUnauthenticatedPrincipal {
                    @Nested
                    class GivenTheEntityIsNew {
                        @Nested
                        class GivenThereIsNoLockOwner {
                            @BeforeEach
                            void setUp() {
                                repo = context.getBean(TestRepository.class);

                                e1 = new TestEntity();
                                e2 = new TestEntity();

                                // ensure the other instantiate fragment is valid
                                OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

                                e3 = new TestEntity();

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
                    class GivenTheEntityExists {
                        @Nested
                        class GivenThereIsNoLockOwner {
                            @BeforeEach
                            void setUp() {
                                repo = context.getBean(TestRepository.class);

                                e1 = new TestEntity();
                                e2 = new TestEntity();

                                // ensure the other instantiate fragment is valid
                                OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

                                e3 = new TestEntity();

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
                class GivenThereIsNoPrincipal {
                    @Nested
                    class GivenTheEntityIsNew {
                        @Nested
                        class GivenThereIsNoLockOwner {
                            @BeforeEach
                            void setUp() {
                                repo = context.getBean(TestRepository.class);

                                e1 = new TestEntity();
                                e2 = new TestEntity();

                                // ensure the other instantiate fragment is valid
                                OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

                                e3 = new TestEntity();

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
                    repo = context.getBean(TestRepository.class);

                    e1 = new TestEntity();
                    e2 = new TestEntity();

                    // ensure the other instantiate fragment is valid
                    OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

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
                    List<TestEntity> results = repo.findAllVersions(e1, Sort.by(Order.desc("id")));
                    assertThat(results.size()).isEqualTo(2);
                    assertThat(results).extracting("xid").contains(e1.getXid(), e1v11.getXid());
                }

                @Test
                void shouldReturnTheOrderedVersionSeries() {
                    List<TestEntity> results = repo.findAllVersions(e1, Sort.by(Order.desc("id")));
                    assertThat(results.size()).isEqualTo(2);
                    assertThat(results).extracting("xid").containsExactly(e1v11.getXid(), e1.getXid());
                }

            }

            @Nested
            class FindAllLatestVersions {
                @BeforeEach
                void setUp() {
                    repo = context.getBean(TestRepository.class);

                    e1 = new TestEntity();
                    e2 = new TestEntity();

                    // ensure the other instantiate fragment is valid
                    OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

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
                    List<TestEntity> results = repo.findAllVersionsLatest((Class<TestEntity>) e1.getClass());
                    assertThat(results).extracting("xid").contains(e1v11.getXid(), e2v2.getXid()).doesNotContain(e3wc.getXid());

                    results = repo.findAllVersionsLatest(TestEntity.class);
                    assertThat(results).extracting("xid").contains(e1v11.getXid(), e2v2.getXid()).doesNotContain(e3wc.getXid());
                }

            }

            @Nested
            class WorkingCopy {
                @Nested
                class WhenTheEntityIsNew {
                    @BeforeEach
                    void setUp() {
                        repo = context.getBean(TestRepository.class);

                        e1 = new TestEntity();
                        e2 = new TestEntity();

                        // ensure the other instantiate fragment is valid
                        OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

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
                class WhenTheEntityExists {
                    @Nested
                    class GivenThePrincipalIsTheLockOwner {
                        @Nested
                        class WhenTheEntityIsNotYetPartOfAVersionTree {
                            @BeforeEach
                            void setUp() {
                                repo = context.getBean(TestRepository.class);

                                e1 = new TestEntity();
                                e2 = new TestEntity();

                                // ensure the other instantiate fragment is valid
                                OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

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
                    class GivenThePrincipalIsNotTheLockOwner {
                        @BeforeEach
                        void setUp() {
                            repo = context.getBean(TestRepository.class);

                            e1 = new TestEntity();
                            e2 = new TestEntity();

                            // ensure the other instantiate fragment is valid
                            OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

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
                    class GivenThePrincipalIsUnauthenticated {
                        @BeforeEach
                        void setUp() {
                            repo = context.getBean(TestRepository.class);

                            e1 = new TestEntity();
                            e2 = new TestEntity();

                            // ensure the other instantiate fragment is valid
                            OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

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
                    class GivenTheEntityIsNotTheCurrentVersion {
                        @BeforeEach
                        void setUp() {
                            repo = context.getBean(TestRepository.class);

                            e1 = new TestEntity();
                            e2 = new TestEntity();

                            // ensure the other instantiate fragment is valid
                            OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

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
                    repo = context.getBean(TestRepository.class);

                    e1 = new TestEntity();
                    e2 = new TestEntity();

                    // ensure the other instantiate fragment is valid
                    OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

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
                    TestEntity wc = repo.workingCopy(e1);
                    assertThat(repo.isPrivateWorkingCopy(wc)).isTrue();
                }

            }

            @Nested
            class FindWorkingCopy {
                @BeforeEach
                void setUp() {
                    repo = context.getBean(TestRepository.class);

                    e1 = new TestEntity();
                    e2 = new TestEntity();

                    // ensure the other instantiate fragment is valid
                    OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

                    setupSecurityContext("some-principal", true);
                }

                @Test
                void shouldReturnTrue() {
                    e1 = repo.save(e1);
                    e1 = repo.lock(e1);
                    TestEntity wc = repo.workingCopy(e1);
                    assertThat(repo.findWorkingCopy(wc)).hasFieldOrPropertyWithValue("xid", wc.getXid());
                }

            }

            @Nested
            class Delete {
                @Nested
                class GivenNoPrincipal {
                    @BeforeEach
                    void setUp() {
                        repo = context.getBean(TestRepository.class);

                        e1 = new TestEntity();
                        e2 = new TestEntity();

                        // ensure the other instantiate fragment is valid
                        OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

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
                class GivenAnUnauthenticatedPrincipal {
                    @BeforeEach
                    void setUp() {
                        repo = context.getBean(TestRepository.class);

                        e1 = new TestEntity();
                        e2 = new TestEntity();

                        // ensure the other instantiate fragment is valid
                        OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

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
                class GivenAPrincipal {
                    @Nested
                    class GivenTheEntityIsNotInAVersionTree {
                        @Nested
                        class GivenThePrincipalIsTheLockOwner {
                            @BeforeEach
                            void setUp() {
                                repo = context.getBean(TestRepository.class);

                                e1 = new TestEntity();
                                e2 = new TestEntity();

                                // ensure the other instantiate fragment is valid
                                OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

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
                        class GivenThePrincipalIsNotTheLockOwner {
                            @BeforeEach
                            void setUp() {
                                repo = context.getBean(TestRepository.class);

                                e1 = new TestEntity();
                                e2 = new TestEntity();

                                // ensure the other instantiate fragment is valid
                                OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

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
                        class GivenThereIsNoLock {
                            @BeforeEach
                            void setUp() {
                                repo = context.getBean(TestRepository.class);

                                e1 = new TestEntity();
                                e2 = new TestEntity();

                                // ensure the other instantiate fragment is valid
                                OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

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
                    class GivenTheEntityIsNotTheHead {
                        @BeforeEach
                        void setUp() {
                            repo = context.getBean(TestRepository.class);

                            e1 = new TestEntity();
                            e2 = new TestEntity();

                            // ensure the other instantiate fragment is valid
                            OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

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
                    class GivenTheEntityIsTheHeadOfAVersionSeriesOf3VersionsAndTheAncestorIsNotAncestralRo {
                        @BeforeEach
                        void setUp() {
                            repo = context.getBean(TestRepository.class);

                            e1 = new TestEntity();
                            e2 = new TestEntity();

                            // ensure the other instantiate fragment is valid
                            OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

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
                            e1v11 = repo.findById(e1v11.getXid()).get();
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
                    class GivenTheEntityIsTheHeadOfAVersionTreeOf2VersionsAncestorIsAncestralRoot {
                        @BeforeEach
                        void setUp() {
                            repo = context.getBean(TestRepository.class);

                            e1 = new TestEntity();
                            e2 = new TestEntity();

                            // ensure the other instantiate fragment is valid
                            OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

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
                            e1 = repo.findById(e1.getXid()).get();
                            assertThat(e1.getXSuccessorId()).isNull();
                        }

                    }

                }

            }

            @Nested
            class DeleteAllVersions {
                @Nested
                class GivenNoPrincipal {
                    @BeforeEach
                    void setUp() {
                        repo = context.getBean(TestRepository.class);

                        e1 = new TestEntity();
                        e2 = new TestEntity();

                        // ensure the other instantiate fragment is valid
                        OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

                        setupSecurityContext("some-principal", true);

                        e1 = repo.save(e1);
                        e1 = repo.lock(e1);
                        e1v11 = repo.version(e1, new VersionInfo("1.1", "Minor"));
                        e1v11 = repo.unlock(e1v11);

                        setupSecurityContext(null, false);
                    }

                    @Test
                    void shouldThrowASecurityException() {
                        try {
                            repo.deleteAllVersions(e1v11);
                            fail("expected security exception");
                        } catch (Exception e) {
                            assertThat(e).isInstanceOf(SecurityException.class);
                        }
                    }

                }

                @Nested
                class GivenAnUnauthenticatedPrincipal {
                    @BeforeEach
                    void setUp() {
                        repo = context.getBean(TestRepository.class);

                        e1 = new TestEntity();
                        e2 = new TestEntity();

                        // ensure the other instantiate fragment is valid
                        OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

                        setupSecurityContext("some-principal", true);

                        e1 = repo.save(e1);
                        e1 = repo.lock(e1);
                        e1v11 = repo.version(e1, new VersionInfo("1.1", "Minor"));
                        e1v11 = repo.unlock(e1v11);

                        setupSecurityContext("some-principal", false);
                    }

                    @Test
                    void shouldThrowASecurityException() {
                        try {
                            repo.deleteAllVersions(e1v11);
                            fail("expected security exception");
                        } catch (Exception e) {
                            assertThat(e).isInstanceOf(SecurityException.class);
                        }
                    }

                }

                @Nested
                class GivenAPrincipal {
                    @Nested
                    class GivenTheEntityIsNotInAVersionTree {
                        @Nested
                        class GivenThePrincipalIsTheLockOwner {
                            @BeforeEach
                            void setUp() {
                                repo = context.getBean(TestRepository.class);

                                e1 = new TestEntity();
                                e2 = new TestEntity();

                                // ensure the other instantiate fragment is valid
                                OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

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
                                repo.findAllVersions(e1v11).forEach((doc) -> {
                                    ids.add(doc.getXid());
                                });

                                repo.deleteAllVersions(e1v11);

                                ids.forEach((id) -> {
                                    assertThat(repo.existsById(id)).isFalse();
                                });
                            }

                        }

                        @Nested
                        class GivenThePrincipalIsNotTheLockOwner {
                            @BeforeEach
                            void setUp() {
                                repo = context.getBean(TestRepository.class);

                                e1 = new TestEntity();
                                e2 = new TestEntity();

                                // ensure the other instantiate fragment is valid
                                OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

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
                                try {
                                    repo.deleteAllVersions(e1v11);
                                    fail("expected lockownerexception");
                                } catch (Exception e) {
                                    assertThat(e).isInstanceOf(LockOwnerException.class);
                                    assertThat(e.getMessage()).contains("not lock owner");
                                }
                            }

                        }

                        @Nested
                        class GivenThereIsNoLock {
                            @BeforeEach
                            void setUp() {
                                repo = context.getBean(TestRepository.class);

                                e1 = new TestEntity();
                                e2 = new TestEntity();

                                // ensure the other instantiate fragment is valid
                                OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

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
                                repo.findAllVersions(e1).forEach((doc) -> {
                                    ids.add(doc.getXid());
                                });

                                repo.deleteAllVersions(e1v11);

                                ids.forEach((id) -> {
                                    assertThat(repo.existsById(id)).isFalse();
                                });
                            }

                        }

                    }

                    @Nested
                    class GivenTheEntityIsNotTheHead {
                        @BeforeEach
                        void setUp() {
                            repo = context.getBean(TestRepository.class);

                            e1 = new TestEntity();
                            e2 = new TestEntity();

                            // ensure the other instantiate fragment is valid
                            OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

                            setupSecurityContext("some-principal", true);

                            e1 = repo.save(e1);
                            e1 = repo.lock(e1);
                            e1v11 = repo.version(e1, new VersionInfo("1.1", "Minor"));
                            e1v11 = repo.unlock(e1v11);

                            setupSecurityContext("some-principal", true);
                        }

                        @Test
                        void shouldFail() {
                            try {
                                repo.deleteAllVersions(e1);
                                fail("expected lockingandversioningexception");
                            } catch (Exception e) {
                                assertThat(e).isInstanceOf(LockingAndVersioningException.class);
                                assertThat(e.getMessage()).contains("not head");
                            }
                        }

                    }

                }

            }

            @Nested
            class Issue2039 {
                @BeforeEach
                void setUp() {
                    repo = context.getBean(TestRepository.class);

                    e1 = new TestEntity();
                    e2 = new TestEntity();

                    // ensure the other instantiate fragment is valid
                    OtherTestEntity ote = otherRepo.save(new OtherTestEntity());

                    setupSecurityContext("some-principal", true);

                    e1 = repo.save(new TestEntity());
                    e2 = repo.save(new TestEntity());
                    e3 = repo.save(new TestEntity());
                }

                @Test
                void shouldReturnTheProvidedEntity() {
                    List<TestEntity> results = repo.findAllVersions(e1, Sort.by(Order.desc("id")));
                    assertThat(results.size()).isEqualTo(1);
                }

                @Test
                void shouldDeleteJustTheProvidedEntity() {
                    repo.deleteAllVersions(e1);

                    Optional<TestEntity> fetched = repo.findById(e1.getXid());
                    assertThat(fetched.isPresent()).isFalse();
                }

            }

        }

    }

    @Configuration
    @EnableJpaRepositories(considerNestedRepositories=true)
    @Import({H2Config.class, JpaLockingAndVersioningConfig.class})
    public static class TestConfig {
    }

    @Configuration
    @EnableTransactionManagement
    public static class H2Config {

        @Bean
        public DataSource dataSource() {
            EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
            return builder.setType(EmbeddedDatabaseType.H2).build();
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            vendorAdapter.setDatabase(Database.H2);
            vendorAdapter.setGenerateDdl(true);

            LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
            factory.setJpaVendorAdapter(vendorAdapter);
            factory.setPackagesToScan(getClass().getPackage().getName());
            factory.setDataSource(dataSource());

            return factory;
        }

        @Bean
        public PlatformTransactionManager transactionManager() {
            JpaTransactionManager txManager = new JpaTransactionManager();
            txManager.setEntityManagerFactory(entityManagerFactory().getObject());
            return txManager;
        }

        @Value("/org/springframework/versions/jpa/schema-drop-h2.sql")
        private ClassPathResource dropVersionSchema;

        @Value("/org/springframework/versions/jpa/schema-h2.sql")
        private ClassPathResource createVersionSchema;

        @Bean
        public DataSourceInitializer datasourceInitializer() {
            ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();

            databasePopulator.addScript(dropVersionSchema);
            databasePopulator.addScript(createVersionSchema);
            databasePopulator.setIgnoreFailedDrops(true);

            DataSourceInitializer initializer = new DataSourceInitializer();
            initializer.setDataSource(dataSource());
            initializer.setDatabasePopulator(databasePopulator);

            return initializer;
        }
    }

    @Entity
    public static class TestEntity {
        @Id @GeneratedValue private Long xid;
        @Version private Long version;
        @AncestorId private Long xAncestorId;
        @AncestorRootId private Long xAncestorRootId;
        @SuccessorId private Long xSuccessorId;
        @LockOwner private String xLockOwner;
        @VersionNumber private String versionNo;
        @VersionLabel private String versionLabel;

        public TestEntity() {}
        public TestEntity(TestEntity entity) {}

        public Long getXid() {
            return xid;
        }

        public void setXid(Long xid) {
            this.xid = xid;
        }

        public Long getVersion() {
            return version;
        }

        public void setVersion(Long version) {
            this.version = version;
        }

        public Long getXAncestorId() {
            return xAncestorId;
        }

        public void setXAncestorId(Long xAncestorId) {
            this.xAncestorId = xAncestorId;
        }

        public Long getXAncestorRootId() {
            return xAncestorRootId;
        }

        public void setXAncestorRootId(Long xAncestorRootId) {
            this.xAncestorRootId = xAncestorRootId;
        }

        public Long getXSuccessorId() {
            return xSuccessorId;
        }

        public void setXSuccessorId(Long xSuccessorId) {
            this.xSuccessorId = xSuccessorId;
        }

        public String getXLockOwner() {
            return xLockOwner;
        }

        public void setXLockOwner(String xLockOwner) {
            this.xLockOwner = xLockOwner;
        }

        public String getVersionNo() {
            return versionNo;
        }

        public void setVersionNo(String versionNo) {
            this.versionNo = versionNo;
        }

        public String getVersionLabel() {
            return versionLabel;
        }

        public void setVersionLabel(String versionLabel) {
            this.versionLabel = versionLabel;
        }
    }

    public interface TestRepository extends JpaRepository<TestEntity, Long>, LockingAndVersioningRepository<TestEntity> {}

    @Entity
    public static class OtherTestEntity {
        @Id @GeneratedValue private Long id;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    public interface OtherTestRepository extends JpaRepository<OtherTestEntity, Long>, LockingAndVersioningRepository<OtherTestEntity> {}

    private static void setupSecurityContext(String principal, boolean isAuthenticated) {
        SecurityContext sc = new SecurityContext() {
            @Override
            public Authentication getAuthentication() {
                return new MockAuthentication(principal, isAuthenticated);
            }

            @Override
            public void setAuthentication(Authentication authentication) {
            }
        };

        SecurityContextHolder.setContext(sc);
    }

    private static class MockAuthentication implements Authentication {

        private final String principal;
        private final boolean isAuthenticated;

        public MockAuthentication(String principal, boolean isAuthenticated) {
            this.principal = principal;
            this.isAuthenticated = isAuthenticated;
        }

        @Override
        public String getName() {
            return principal;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return null;
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
        public boolean isAuthenticated() {
            return isAuthenticated;
        }

        @Override
        public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        }
    }

}
