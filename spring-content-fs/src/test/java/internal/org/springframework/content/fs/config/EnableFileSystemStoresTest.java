package internal.org.springframework.content.fs.config;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.fs.config.EnableFileSystemStores;
import org.springframework.content.fs.config.FileSystemStoreConfigurer;
import org.springframework.content.fs.config.FileSystemStoreConverter;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.UUID;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.AfterEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class EnableFileSystemStoresTest {

    private AnnotationConfigApplicationContext context;

    // mocks
    static FileSystemStoreConfigurer configurer;

    {
        Describe("EnableFileSystemStores", () -> {

            Context("given a context and a configuration with a filesystem content repository bean",
                    () -> {
                        BeforeEach(() -> {
                            context = new AnnotationConfigApplicationContext();
                            context.register(TestConfig.class);
                            context.refresh();
                        });
                        AfterEach(() -> context.close());
                        It("should have a ContentRepository bean", () ->
                                assertThat(context.getBean(TestEntityContentRepository.class), is(not(nullValue()))));
                        It("should have a filesystem placement service bean", () ->
                                assertThat(context.getBean("filesystemStorePlacementService"), is(not(nullValue()))));
                        It("should have a FileSystemResourceLoader bean", () ->
                                assertThat(context.getBean("fileSystemResourceLoader"), is(not(nullValue()))));
                    });

            Context("given a context with a configurer", () -> {
                BeforeEach(() -> {
                    configurer = mock(FileSystemStoreConfigurer.class);

                    context = new AnnotationConfigApplicationContext();
                    context.register(ConverterConfig.class);
                    context.refresh();
                });
                AfterEach(() -> context.close());
                It("should call that configurer to help customize the store", () ->
                        verify(configurer).configureFileSystemStoreConverters(any()));
            });

            Context("given a context with an empty configuration", () -> {
                BeforeEach(() -> {
                    context = new AnnotationConfigApplicationContext();
                    context.register(EmptyConfig.class);
                    context.refresh();
                });
                AfterEach(() -> context.close());
                It("should not contain any filesystem repository beans", () -> {
                    try {
                        context.getBean(TestEntityContentRepository.class);
                        fail("expected no such bean");
                    } catch (NoSuchBeanDefinitionException e) {
                        assertThat(true, is(true));
                    }
                });
            });
        });
    }

    @Test
    public void noop() {
    }

    @Configuration
    @EnableFileSystemStores(basePackages = "contains.no.fs.repositories")
    @PropertySource("classpath:/test.properties")
    public static class EmptyConfig {
    }

    @Configuration
    @EnableFileSystemStores
    @PropertySource("classpath:/test.properties")
    public static class TestConfig {

        @Value("${spring.content.fs.filesystemRoot:#{null}}")
        private String filesystemRoot;

        @Bean
        FileSystemResourceLoader fileSystemResourceLoader() {
            return new FileSystemResourceLoader(filesystemRoot);
        }
    }

    @Configuration
    @EnableFileSystemStores
    @PropertySource("classpath:/test.properties")
    public static class ConverterConfig {

        @Value("${spring.content.fs.filesystemRoot:#{null}}")
        private String filesystemRoot;

        @Bean
        public FileSystemStoreConverter<UUID, String> uuidConverter() {
            return source -> String.format("/%s", source.toString().replaceAll("-", "/"));
        }

        @Bean
        public FileSystemStoreConfigurer configurer() {
            return configurer;
        }

        @Bean
        FileSystemResourceLoader fileSystemResourceLoader() {
            return new FileSystemResourceLoader(filesystemRoot);
        }
    }

    public static class TestEntity {
        @ContentId
        private String contentId;
    }

    public interface TestEntityContentRepository
            extends ContentStore<TestEntity, String> {
    }
}
