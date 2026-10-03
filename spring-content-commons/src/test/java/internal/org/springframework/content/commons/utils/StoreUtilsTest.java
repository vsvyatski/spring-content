package internal.org.springframework.content.commons.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.ResourceLoader;

public class StoreUtilsTest {

    private StoreCandidateComponentProvider scanner;
    private Environment env;
    private ResourceLoader loader;
    private String[] basePackages;
    private boolean multiStoreMode;
    private Class<?>[] identifyingTypes;
    private String registrarId;

    private GenericBeanDefinition def1;
    private GenericBeanDefinition def2;

    
    @Nested
    class GetStoreCandidates {
        @Nested
        class WhenMultipleStorageBeansAreFound {
            @Nested
            class WhenMultipleStorageModulesAreFoundOnTheClasspath {
                @BeforeEach
                void setUp() {
                    env = mock(Environment.class);
                    loader = new FileSystemResourceLoader();
                    ((FileSystemResourceLoader)loader).setClassLoader(this.getClass().getClassLoader());
                    basePackages = new String[] {StoreUtilsTest.class.getPackage().getName()};
                    registrarId = "test";

                    def1 = new GenericBeanDefinition();
                    def1.setBeanClassName(StoreBean1.class.getName());
                    def2 = new GenericBeanDefinition();
                    def2.setBeanClassName(StoreBean2.class.getName());

                    Set<BeanDefinition> defs = new HashSet();
                    defs.add(def1);
                    defs.add(def2);

                    scanner = mock(StoreCandidateComponentProvider.class);
                    when(scanner.findCandidateComponents(org.mockito.ArgumentMatchers.any())).thenReturn(defs);

                    identifyingTypes = new Class<?>[] {StoreUtilsTest.StoreType1.class };

                    multiStoreMode = true;

                }

                @Test
                void shouldReturnTheStoreMatchingTheIdentifyingType() {
                    Set<GenericBeanDefinition> beans = StoreUtils.getStoreCandidates(scanner, env, loader, basePackages, multiStoreMode, identifyingTypes, registrarId);
                    assertThat(beans).contains(def1);
                    assertThat(beans).doesNotContain(def2);

                }

                @Nested
                class WhenTheStoreBeansCantBeMatchedAgainstSignatureTypesButTheRegistrarMatchesTheDefa {
                    @BeforeEach
                    void setUp() {
                        env = mock(Environment.class);
                        loader = new FileSystemResourceLoader();
                        ((FileSystemResourceLoader)loader).setClassLoader(this.getClass().getClassLoader());
                        basePackages = new String[] {StoreUtilsTest.class.getPackage().getName()};
                        registrarId = "test";

                        def1 = new GenericBeanDefinition();
                        def1.setBeanClassName(StoreBean1.class.getName());
                        def2 = new GenericBeanDefinition();
                        def2.setBeanClassName(StoreBean2.class.getName());

                        Set<BeanDefinition> defs = new HashSet();
                        defs.add(def1);
                        defs.add(def2);

                        scanner = mock(StoreCandidateComponentProvider.class);
                        when(scanner.findCandidateComponents(org.mockito.ArgumentMatchers.any())).thenReturn(defs);

                        identifyingTypes = new Class<?>[] {StoreUtilsTest.StoreType1.class };

                        multiStoreMode = true;

                        multiStoreMode = true;

                        identifyingTypes = new Class<?>[] {};

                        when(env.getProperty("spring.content.storage.type.default")).thenReturn("test");

                    }

                    @Test
                    void shouldReturnAllStores() {
                        Set<GenericBeanDefinition> beans = StoreUtils.getStoreCandidates(scanner, env, loader, basePackages, multiStoreMode, identifyingTypes, registrarId);
                        assertThat(beans).contains(def1);
                        assertThat(beans).contains(def2);

                    }

                }

                @Nested
                class WhenTheStoreBeansCantBeMatchedAgainstSignatureTypesAndTheRegistrarDoesnTMatchThe {
                    @BeforeEach
                    void setUp() {
                        env = mock(Environment.class);
                        loader = new FileSystemResourceLoader();
                        ((FileSystemResourceLoader)loader).setClassLoader(this.getClass().getClassLoader());
                        basePackages = new String[] {StoreUtilsTest.class.getPackage().getName()};
                        registrarId = "test";

                        def1 = new GenericBeanDefinition();
                        def1.setBeanClassName(StoreBean1.class.getName());
                        def2 = new GenericBeanDefinition();
                        def2.setBeanClassName(StoreBean2.class.getName());

                        Set<BeanDefinition> defs = new HashSet();
                        defs.add(def1);
                        defs.add(def2);

                        scanner = mock(StoreCandidateComponentProvider.class);
                        when(scanner.findCandidateComponents(org.mockito.ArgumentMatchers.any())).thenReturn(defs);

                        identifyingTypes = new Class<?>[] {StoreUtilsTest.StoreType1.class };

                        multiStoreMode = true;

                        multiStoreMode = true;

                        identifyingTypes = new Class<?>[] {};
                        registrarId = "other-id";

                        when(env.getProperty("spring.content.storage.type.default")).thenReturn("test");

                    }

                    @Test
                    void shouldnTReturnAnyStores() {
                        Set<GenericBeanDefinition> beans = StoreUtils.getStoreCandidates(scanner, env, loader, basePackages, multiStoreMode, identifyingTypes, registrarId);
                        assertThat(beans).doesNotContain(def1);
                        assertThat(beans).doesNotContain(def2);

                    }

                }

            }

            @Nested
            class WhenMultiModeIsFalse {
                @BeforeEach
                void setUp() {
                    env = mock(Environment.class);
                    loader = new FileSystemResourceLoader();
                    ((FileSystemResourceLoader)loader).setClassLoader(this.getClass().getClassLoader());
                    basePackages = new String[] {StoreUtilsTest.class.getPackage().getName()};
                    registrarId = "test";

                    def1 = new GenericBeanDefinition();
                    def1.setBeanClassName(StoreBean1.class.getName());
                    def2 = new GenericBeanDefinition();
                    def2.setBeanClassName(StoreBean2.class.getName());

                    Set<BeanDefinition> defs = new HashSet();
                    defs.add(def1);
                    defs.add(def2);

                    scanner = mock(StoreCandidateComponentProvider.class);
                    when(scanner.findCandidateComponents(org.mockito.ArgumentMatchers.any())).thenReturn(defs);

                    identifyingTypes = new Class<?>[] {StoreUtilsTest.StoreType1.class };

                    multiStoreMode = false;

                }

                @Test
                void shouldReturnAllStores() {
                    Set<GenericBeanDefinition> beans = StoreUtils.getStoreCandidates(scanner, env, loader, basePackages, multiStoreMode, identifyingTypes, "test");
                    assertThat(beans).contains(def1);
                    assertThat(beans).contains(def2);

                }

            }

        }

    }

    interface StoreType1 {
    }

    interface StoreType2 {
    }

    interface StoreBean1 extends StoreType1 {}
    interface StoreBean2 extends StoreType2 {}
}
