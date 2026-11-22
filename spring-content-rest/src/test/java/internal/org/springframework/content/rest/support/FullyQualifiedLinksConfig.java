package internal.org.springframework.content.rest.support;

import internal.org.springframework.content.rest.support.config.JpaInfrastructureConfig;
import org.springframework.content.fs.config.EnableFileSystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.rest.config.ContentRestConfigurer;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.io.File;

@Configuration
@Import(TestRenditionConfig.class)
@EnableJpaRepositories(basePackages = "internal.org.springframework.content.rest.support")
@EnableTransactionManagement
@EnableFileSystemStores(basePackages = "internal.org.springframework.content.rest.support")
public class FullyQualifiedLinksConfig extends JpaInfrastructureConfig {

    @Bean
    public ContentRestConfigurer contentRestConfigurer() {

        return new ContentRestConfigurer() {

            @Override
            public void configure(RestConfiguration config) {
                config.setFullyQualifiedLinks(true);
            }
        };
    }

    @Bean
    FileSystemResourceLoader fileSystemResourceLoader() {
        return new FileSystemResourceLoader(filesystemRoot().getAbsolutePath());
    }

    @Bean
    public File filesystemRoot() {
        File baseDir = new File(System.getProperty("java.io.tmpdir"));
        File filesystemRoot = new File(baseDir, "spring-content-controller-tests");
        if (!filesystemRoot.mkdirs() && !filesystemRoot.isDirectory()) {
            throw new IllegalStateException("Failed to create " + filesystemRoot);
        }
        return filesystemRoot;
    }
}
