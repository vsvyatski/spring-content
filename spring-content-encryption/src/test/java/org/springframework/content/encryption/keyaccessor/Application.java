package org.springframework.content.encryption.keyaccessor;

import org.springframework.boot.SpringApplication;
import org.springframework.content.encryption.support.EncryptionFsInfrastructureConfig;
import org.springframework.content.encryption.support.EncryptionFsRestBootApplication;
import org.springframework.content.fs.config.EnableFileSystemStores;
import org.springframework.content.test.support.TestSupportPackages;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EncryptionFsRestBootApplication
@EnableJpaRepositories(basePackages = TestSupportPackages.ENCRYPTION, considerNestedRepositories = true)
@EnableFileSystemStores(basePackages = "org.springframework.content.encryption.fs")
@Import({EncryptionFsInfrastructureConfig.class, KeyAccessorEncryptionConfigurer.class})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}