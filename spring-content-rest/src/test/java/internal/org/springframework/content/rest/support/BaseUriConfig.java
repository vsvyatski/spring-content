package internal.org.springframework.content.rest.support;

import internal.org.springframework.content.rest.support.config.JpaInfrastructureConfig;
import org.jspecify.annotations.NonNull;
import org.springframework.content.fs.config.EnableFileSystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.rest.config.ContentRestConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import java.io.File;
import java.net.URI;

@Configuration
@Import(TestRenditionConfig.class)
@EnableJpaRepositories(basePackages = "internal.org.springframework.content.rest.support")
@EnableTransactionManagement
@EnableFileSystemStores(basePackages = "internal.org.springframework.content.rest.support")
public class BaseUriConfig extends JpaInfrastructureConfig {

    @Bean
    FileSystemResourceLoader fileSystemResourceLoader() {
        return new FileSystemResourceLoader(filesystemRoot().getAbsolutePath());
    }

    @Bean
    public File filesystemRoot() {
        File baseDir = new File(System.getProperty("java.io.tmpdir"));
        File filesystemRoot = new File(baseDir, "spring-content-links-tests");
        if (!filesystemRoot.mkdirs() && !filesystemRoot.isDirectory()) {
            throw new IllegalStateException("Failed to create " + filesystemRoot);
        }
        return filesystemRoot;
    }

    @Bean
    public RepositoryRestConfigurer repositoryRestConfigurer() {
        return new RepositoryRestConfigurer() {

            @Override
            public void configureRepositoryRestConfiguration(@NonNull RepositoryRestConfiguration config,
                                                             @NonNull CorsRegistry cors) {
                config.setBasePath("/api");
            }
        };
    }

    @Bean
    public ContentRestConfigurer configurer() {
        return config -> config.setBaseUri(URI.create("/contentApi"));
    }
}
