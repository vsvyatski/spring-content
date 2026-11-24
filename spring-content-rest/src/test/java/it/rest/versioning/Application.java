package it.rest.versioning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.content.fs.config.EnableFileSystemStores;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.versions.jpa.config.JpaLockingAndVersioningConfig;
import org.springframework.versions.test.support.VersioningTestPackages;

@SpringBootApplication
@EntityScan(basePackages = VersioningTestPackages.JPA)
@EnableJpaRepositories(considerNestedRepositories = true,
    basePackages = {"it.rest.versioning", "org.springframework.versions"})
@EnableTransactionManagement
@EnableFileSystemStores(basePackages = "it.rest.versioning")
@Import({JpaLockingAndVersioningConfig.class, RestConfiguration.class, VersioningRestSecurityConfiguration.class})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
