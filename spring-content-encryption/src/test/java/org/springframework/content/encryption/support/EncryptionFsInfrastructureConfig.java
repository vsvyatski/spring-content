package org.springframework.content.encryption.support;

import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;

/**
 * File-system store root for encryption REST integration tests.
 *
 * @author Vladimir Svyatski
 */
@Configuration
public class EncryptionFsInfrastructureConfig {

    @Bean
    public java.io.File filesystemRoot() {
        try {
            return Files.createTempDirectory("").toFile();
        } catch (IOException ignored) {
        }
        return null;
    }

    @Bean
    public FileSystemResourceLoader fileSystemResourceLoader(java.io.File filesystemRoot) {
        return new FileSystemResourceLoader(filesystemRoot.getAbsolutePath());
    }
}