package internal.org.springframework.content.rest.it.boot;

import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;

/**
 * File-system store root for REST integration tests that use
 * {@link org.springframework.content.fs.config.EnableFileSystemStores}.
 *
 * @author Vladimir Svyatski
 */
@Configuration
public class TemporaryFileSystemResourceLoaderConfig {

    @Bean
    public FileSystemResourceLoader fileSystemResourceLoader() throws IOException {
        return new FileSystemResourceLoader(Files.createTempDirectory("").toFile().getAbsolutePath());
    }
}