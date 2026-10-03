package internal.org.springframework.versions.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.versions.*;

import static org.assertj.core.api.Assertions.assertThat;

public class JpaVersioningServiceImplTest {

    private VersioningService versioner;

    private Object result;

    private EntityManager em;

    private TestEntity entity;
    private TestEntity successor;

    @Nested
    class JpaVersioningServiceImplCases {
        @Nested
        class EstablishAncestralRoot {
            @BeforeEach
            void setUp() {
                entity = new TestEntity();

                versioner = new JpaVersioningServiceImpl(em);

                result = versioner.establishAncestralRoot(entity);
            }

            @Test
            void shouldSetTheAncestorIdToNull() {
                assertThat(entity.getAncestorId()).isNull();
            }

            @Test
            void shouldSetTheAncestorRootIdToItSOwnId() {
                assertThat(entity.getAncestorRootId()).isEqualTo(entity.getId());
            }

            @Test
            void shouldReturnTheEntity() {
                assertThat(result).isEqualTo(entity);
            }
        }

        @Nested
        class EstablishAncestor {
            @BeforeEach
            void setUp() {
                entity = new TestEntity();
                successor = new TestEntity();
                successor.setId(999L);

                versioner = new JpaVersioningServiceImpl(em);

                result = versioner.establishAncestor(entity, successor);
            }

            @Test
            void shouldSetTheSuccessorIdToNull() {
                assertThat(entity.getSuccessorId()).isEqualTo(999L);
            }

            @Test
            void shouldReturnTheEntity() {
                assertThat(result).isEqualTo(entity);
            }
        }

        @Nested
        class EstablishSuccessor {
            @BeforeEach
            void setUp() {
                successor = new TestEntity();
                TestEntity ancestralRoot = new TestEntity();
                ancestralRoot.setId(1234L);
                TestEntity ancestor = new TestEntity();
                ancestor.setId(5678L);
                String versionNo = "1.1";
                String versionLabel = "a new version";

                versioner = new JpaVersioningServiceImpl(em);

                result = versioner.establishSuccessor(successor, versionNo, versionLabel, ancestralRoot, ancestor);
            }

            @Test
            void shouldSetTheVersionNumber() {
                assertThat(successor.getVersionNo()).isEqualTo("1.1");
            }

            @Test
            void shouldSetTheVersionLabel() {
                assertThat(successor.getVersionLabel()).isEqualTo("a new version");
            }

            @Test
            void shouldSetTheSuccessorIdToNull() {
                assertThat(successor.getSuccessorId()).isNull();
            }

            @Test
            void shouldSetTheAncestorRootId() {
                assertThat(successor.getAncestorRootId()).isEqualTo(1234L);
            }

            @Test
            void shouldSetTheAncestorId() {
                assertThat(successor.getAncestorId()).isEqualTo(5678L);
            }

            @Test
            void shouldReturnTheEntity() {
                assertThat(result).isEqualTo(successor);
            }
        }
    }

    private static class TestEntity {
        @Id
        private Long id;
        @Version
        private Long version;
        @AncestorId
        private Long ancestorId;
        @AncestorRootId
        private Long ancestorRootId;
        @SuccessorId
        private Long successorId;
        @VersionNumber
        private String versionNo;
        @VersionLabel
        private String versionLabel;

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
