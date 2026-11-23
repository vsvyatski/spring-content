package internal.org.springframework.versions.jpa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.versions.test.support.VersioningUnitFields;

import static org.assertj.core.api.Assertions.assertThat;

public class JpaVersioningServiceImplTest {

    private VersioningService versioner;

    private Object result;

    private VersioningUnitFields entity;
    private VersioningUnitFields successor;

    @Nested
   class JpaVersioningServiceImplCases {
        @Nested
        class EstablishAncestralRoot {
            @BeforeEach
            void setUp() {
                entity = new VersioningUnitFields();
                entity.setId(100L);

                versioner = new JpaVersioningServiceImpl(null);

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
                entity = new VersioningUnitFields();
                successor = new VersioningUnitFields();
                successor.setId(999L);

                versioner = new JpaVersioningServiceImpl(null);

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
                successor = new VersioningUnitFields();
                VersioningUnitFields ancestralRoot = new VersioningUnitFields();
                ancestralRoot.setId(1234L);
                VersioningUnitFields ancestor = new VersioningUnitFields();
                ancestor.setId(5678L);
                String versionNo = "1.1";
                String versionLabel = "a new version";

                versioner = new JpaVersioningServiceImpl(null);

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
}
