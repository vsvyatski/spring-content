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
            void shouldSetAncestorIdToNull() {
                assertThat(entity.getAncestorId()).isNull();
            }

            @Test
            void shouldSetAncestorRootIdToItSOwnId() {
                assertThat(entity.getAncestorRootId()).isEqualTo(entity.getId());
            }

            @Test
            void shouldReturnEntity() {
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
            void shouldSetSuccessorIdToNull() {
                assertThat(entity.getSuccessorId()).isEqualTo(999L);
            }

            @Test
            void shouldReturnEntity() {
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
            void shouldSetVersionNumber() {
                assertThat(successor.getVersionNo()).isEqualTo("1.1");
            }

            @Test
            void shouldSetVersionLabel() {
                assertThat(successor.getVersionLabel()).isEqualTo("a new version");
            }

            @Test
            void shouldSetSuccessorIdToNull() {
                assertThat(successor.getSuccessorId()).isNull();
            }

            @Test
            void shouldSetAncestorRootId() {
                assertThat(successor.getAncestorRootId()).isEqualTo(1234L);
            }

            @Test
            void shouldSetAncestorId() {
                assertThat(successor.getAncestorId()).isEqualTo(5678L);
            }

            @Test
            void shouldReturnEntity() {
                assertThat(result).isEqualTo(successor);
            }
        }
    }
}
