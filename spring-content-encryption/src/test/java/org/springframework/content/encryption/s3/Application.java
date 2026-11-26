package org.springframework.content.encryption.s3;

import internal.org.springframework.content.fs.boot.autoconfigure.FileSystemContentAutoConfiguration;
import internal.org.springframework.content.rest.boot.autoconfigure.ContentRestAutoConfiguration;
import org.apache.commons.lang3.ArrayUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.content.encryption.LocalStack;
import org.springframework.content.encryption.VaultContainerSupport;
import org.springframework.content.encryption.config.EncryptingContentStoreConfigurer;
import org.springframework.content.encryption.engine.ContentEncryptionEngine.EncryptionParameters;
import org.springframework.content.encryption.keys.DataEncryptionKeyWrapper;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey.EncryptedSymmetricDataEncryptionKey;
import org.springframework.content.encryption.keys.VaultTransitDataEncryptionKeyWrapper;
import org.springframework.content.s3.config.EnableS3Stores;
import org.springframework.content.test.support.TestSupportPackages;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.config.AbstractVaultConfiguration;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URISyntaxException;
import java.util.List;

@SpringBootApplication(exclude = {FileSystemContentAutoConfiguration.class})
@ImportAutoConfiguration(ContentRestAutoConfiguration.class)
@EnableJpaRepositories(basePackages = TestSupportPackages.ENCRYPTION, considerNestedRepositories = true)
@EnableS3Stores(basePackages = "org.springframework.content.encryption.s3")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Configuration
    public static class AppConfig extends AbstractVaultConfiguration {

        @Override
        public @NonNull VaultEndpoint vaultEndpoint() {

            String host = VaultContainerSupport.getVaultContainer().getHost();
            int port = VaultContainerSupport.getVaultContainer().getFirstMappedPort();

            VaultEndpoint vault = VaultEndpoint.create(host, port);
            vault.setScheme("http");
            return vault;
        }

        @Override
        public @NonNull ClientAuthentication clientAuthentication() {
            return new TokenAuthentication("my-root-token");
        }

        @Bean
        public S3Client amazonS3() throws URISyntaxException {
            return LocalStack.getAmazonS3Client();
        }

        @Bean
        public EncryptingContentStoreConfigurer<EncryptedS3FileStore> config() {
            return config -> config.encryptionKeyContentProperty("key")
                .dataEncryptionKeyWrappers(List.of(
                    new EncryptingOnlyKeyWrapper(),
                    new VaultTransitDataEncryptionKeyWrapper(
                        vaultTemplate().opsForTransit(),
                        "my-key"
                    )
                ));
        }
    }

    static class EncryptingOnlyKeyWrapper implements DataEncryptionKeyWrapper<EncryptedSymmetricDataEncryptionKey> {
        @Override
        public boolean supports(StoredDataEncryptionKey storedDataEncryptionKey) {
            return false;
        }

        @Override
        public EncryptedSymmetricDataEncryptionKey wrapEncryptionKey(EncryptionParameters dataEncryptionParameters) {
            return new EncryptedSymmetricDataEncryptionKey("drop", "", "", dataEncryptionParameters.secretKey().getAlgorithm(),
                ArrayUtils.EMPTY_BYTE_ARRAY, ArrayUtils.EMPTY_BYTE_ARRAY);
        }

        @Override
        public EncryptionParameters unwrapEncryptionKey(
            EncryptedSymmetricDataEncryptionKey encryptedDataEncryptionKey) {
            throw new IllegalArgumentException("Can not unwrap encryption key");
        }
    }
}
