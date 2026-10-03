package internal.org.springframework.versions.jpa;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.versions.LockingAndVersioningException;

public class JpaCloningServiceImplTest {

    private JpaCloningServiceImpl cloner;

    private Object entity;
    private Object result;

    private Exception e;

    
    @Nested
    class JpaCloningServiceImplCases {
        @Nested
        class Clone {
            @Nested
            class GivenAnEntityWithACopyConstructor {
                @BeforeEach
                void setUp() {
                    entity = new TestEntity();

                    cloner = new JpaCloningServiceImpl();

                    try {
                        result = cloner.clone(entity);
                    } catch (Exception e) {
                        JpaCloningServiceImplTest.this.e = e;
                    }
                }

                @Test
                void shouldCloneTheEntity() {
                    assertThat(result).isNotNull();
                    assertThat(result).isNotEqualTo(entity);
                }

            }

            @Nested
            class GivenAnEntityWithACopyConstructor2 {
                @BeforeEach
                void setUp() {
                    entity = new NoCopyConstructorTestEntity();

                    cloner = new JpaCloningServiceImpl();

                    try {
                        result = cloner.clone(entity);
                    } catch (Exception e) {
                        JpaCloningServiceImplTest.this.e = e;
                    }
                }

                @Test
                void shouldCloneTheEntity() {
                    assertThat(e).isInstanceOf(LockingAndVersioningException.class);
                    assertThat(e.getMessage()).contains("no copy constructor");
                }

            }

            @Nested
            class GivenAnEntityWithAFailingCopyConstructor {
                @BeforeEach
                void setUp() {
                    entity = new FailingCopyConstructorTestEntity();

                    cloner = new JpaCloningServiceImpl();

                    try {
                        result = cloner.clone(entity);
                    } catch (Exception e) {
                        JpaCloningServiceImplTest.this.e = e;
                    }
                }

                @Test
                void shouldCloneTheEntity() {
                    assertThat(e).isInstanceOf(LockingAndVersioningException.class);
                    assertThat(e.getMessage()).contains("copy constructor failed");
                }

            }

        }

    }

    public static class TestEntity {
        public TestEntity() {}

        public TestEntity(TestEntity entity) {
        }
    }

    public static class NoCopyConstructorTestEntity {
        public NoCopyConstructorTestEntity() {}
    }

    public static class FailingCopyConstructorTestEntity {
        public FailingCopyConstructorTestEntity() {}

        public FailingCopyConstructorTestEntity(FailingCopyConstructorTestEntity entity) {
            throw new RuntimeException();
        }
    }
}
