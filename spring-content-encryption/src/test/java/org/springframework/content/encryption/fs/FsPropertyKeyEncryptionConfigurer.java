package org.springframework.content.encryption.fs;

import org.springframework.content.encryption.config.EncryptingContentStoreConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Vladimir Svyatski
 */
@Configuration
public class FsPropertyKeyEncryptionConfigurer {

    @Bean
    public EncryptingContentStoreConfigurer<EncryptedFsFileStore> config() {
        return config -> config.encryptionKeyContentProperty("key");
    }
}