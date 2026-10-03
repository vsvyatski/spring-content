package internal.org.springframework.versions.jpa;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.versions.AncestorId;
import org.springframework.versions.AncestorRootId;
import org.springframework.versions.SuccessorId;
import org.springframework.versions.VersionLabel;
import org.springframework.versions.VersionNumber;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.Version;


public class JpaVersioningServiceImplTest {

    private VersioningService versioner;

    private Object result;

    private EntityManager em;

    private TestEntity entity, successor, ancestralRoot, ancestor;
    private String versionNo, versionLabel;

    
    @Nested
    class JpaVersioningServiceImplCases {
        @Nested
        class EstablishAncestralRoot {
            @BeforeEach
            void setUp() throws Throwable {
                entity = new TestEntity();

                versioner = new JpaVersioningServiceImpl(em);

                result = versioner.establishAncestralRoot(entity);
            }
            @Test
            void shouldSetTheAncestorIdToNull() throws Throwable {
                assertThat(entity.getAncestorId()).isNull();
            }
            @Test
            void shouldSetTheAncestorRootIdToItSOwnId() throws Throwable {
                assertThat(entity.getAncestorRootId()).isEqualTo(entity.getId());
            }
            @Test
            void shouldReturnTheEntity() throws Throwable {
                assertThat(result).isEqualTo(entity);
            }
        }
        @Nested
        class EstablishAncestor {
            @BeforeEach
            void setUp() throws Throwable {
                entity = new TestEntity();
                                    successor = new TestEntity();
                                    successor.setId(999L);

                versioner = new JpaVersioningServiceImpl(em);

                result = versioner.establishAncestor(entity, successor);
            }
            @Test
            void shouldSetTheSuccessorIdToNull() throws Throwable {
                assertThat(entity.getSuccessorId()).isEqualTo(999L);
            }
            @Test
            void shouldReturnTheEntity() throws Throwable {
                assertThat(result).isEqualTo(entity);
            }
        }
        @Nested
        class EstablishSuccessor {
            @BeforeEach
            void setUp() throws Throwable {
                successor = new TestEntity();
                                    ancestralRoot = new TestEntity();
                                    ancestralRoot.setId(1234L);
                                    ancestor = new TestEntity();
                                    ancestor.setId(5678L);
                                    versionNo = "1.1";
                                    versionLabel = "a new version";

                versioner = new JpaVersioningServiceImpl(em);

                result = versioner.establishSuccessor(successor, versionNo, versionLabel, ancestralRoot, ancestor);
            }
            @Test
            void shouldSetTheVersionNumber() throws Throwable {
                assertThat(successor.getVersionNo()).isEqualTo("1.1");
            }
            @Test
            void shouldSetTheVersionLabel() throws Throwable {
                assertThat(successor.getVersionLabel()).isEqualTo("a new version");
            }
            @Test
            void shouldSetTheSuccessorIdToNull() throws Throwable {
                assertThat(successor.getSuccessorId()).isNull();
            }
            @Test
            void shouldSetTheAncestorRootId() throws Throwable {
                assertThat(successor.getAncestorRootId()).isEqualTo(1234L);
            }
            @Test
            void shouldSetTheAncestorId() throws Throwable {
                assertThat(successor.getAncestorId()).isEqualTo(5678L);
            }
            @Test
            void shouldReturnTheEntity() throws Throwable {
                assertThat(result).isEqualTo(successor);
            }
        }
    }


    private class TestEntity {
        @Id private Long id;
        @Version private Long version;
        @AncestorId private Long ancestorId;
        @AncestorRootId private Long ancestorRootId;
        @SuccessorId private Long successorId;
        @VersionNumber private String versionNo;
        @VersionLabel private String versionLabel;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getVersion() {
            return version;
        }

        public void setVersion(Long version) {
            this.version = version;
        }

        public Long getAncestorId() {
            return ancestorId;
        }

        public void setAncestorId(Long ancestorId) {
            this.ancestorId = ancestorId;
        }

        public Long getAncestorRootId() {
            return ancestorRootId;
        }

        public void setAncestorRootId(Long ancestorRootId) {
            this.ancestorRootId = ancestorRootId;
        }

        public Long getSuccessorId() {
            return successorId;
        }

        public void setSuccessorId(Long successorId) {
            this.successorId = successorId;
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
}
