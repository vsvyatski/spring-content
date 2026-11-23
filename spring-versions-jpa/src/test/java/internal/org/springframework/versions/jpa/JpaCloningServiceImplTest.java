package internal.org.springframework.versions.jpa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.versions.LockingAndVersioningException;

import static org.assertj.core.api.Assertions.assertThat;

public class JpaCloningServiceImplTest {

    private JpaCloningServiceImpl cloner;

    private Object entity;
    private Object result;

    private Exception e;

    public static class TestEntity {
        protected short someField;

        public TestEntity() {
        }

        public TestEntity(TestEntity entity) {
            someField = entity.someField;
        }
    }

    public static class NoCopyConstructorTestEntity {
        public NoCopyConstructorTestEntity() {
        }
    }

    public static class FailingCopyConstructorTestEntity {
        protected String someStringThatMustBeSetToSomething;

        public FailingCopyConstructorTestEntity() {
        }

        @SuppressWarnings("unused")
        public FailingCopyConstructorTestEntity(FailingCopyConstructorTestEntity entity) {
            if (entity.someStringThatMustBeSetToSomething == null)
                throw new IllegalArgumentException("The incoming entity seems uninitialized.");
            someStringThatMustBeSetToSomething = entity.someStringThatMustBeSetToSomething;
        }
    }

    @Nested
   class JpaCloningServiceImplCases {
        @Nested
        class Clone {
            @Nested
            @DisplayName("Given an entity with a copy constructor")
            class EntityWithCopyConstructor {
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

            @DisplayName("Given an entity with a copy constructor2")

            class EntityWithCopyConstructor2 {
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

            @DisplayName("Given an entity with a failing copy constructor")

            class EntityWithFailingCopyConstructor {
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
}
