package internal.org.springframework.content.fs.boot.autoconfigure;

import internal.org.springframework.content.fs.config.FileSystemStoreConfiguration;
import internal.org.springframework.content.fs.config.FileSystemStoreFactoryBean;
import internal.org.springframework.content.fs.config.FileSystemStoreRegistrar;
import internal.org.springframework.versions.jpa.boot.autoconfigure.JpaVersionsAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;

@AutoConfiguration
@AutoConfigureAfter({JpaVersionsAutoConfiguration.class})
@ConditionalOnClass(FileSystemStoreRegistrar.class)
@ConditionalOnProperty(prefix = "spring.content.storage.type", name = "default",
        havingValue = "fs", matchIfMissing = true)
public class FileSystemContentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(value = FileSystemResourceLoader.class, name = "fileSystemResourceLoader")
    FileSystemResourceLoader fileSystemResourceLoader(FileSystemProperties props) {
        return new FileSystemResourceLoader(props.getFileSystemRoot());
    }

    @Configuration
    @ConditionalOnMissingBean(FileSystemStoreFactoryBean.class)
    @Import({FileSystemContentAutoConfigureRegistrar.class, FileSystemStoreConfiguration.class})
    public static class EnableFileSystemStoresConfig {
    }

    @Component
    @ConfigurationProperties(prefix = "spring.content.fs")
    public static class FileSystemProperties {

        private static final Logger logger = LoggerFactory.getLogger(FileSystemProperties.class);

        /**
         * The root location where file system stores place their content
         */
        private String fileSystemRoot;

        public String getFileSystemRoot() {
            if (fileSystemRoot == null) {
                try {
                    fileSystemRoot = Files.createTempDirectory(null).toString();
                } catch (IOException ioe) {
                    logger.error("Unexpected error, defaulting filesystem root to NULL", ioe);
                }
            }

            return this.fileSystemRoot;
        }

        public void setFileSystemRoot(String fileSystemRoot) {
            this.fileSystemRoot = fileSystemRoot;
        }
    }
}
