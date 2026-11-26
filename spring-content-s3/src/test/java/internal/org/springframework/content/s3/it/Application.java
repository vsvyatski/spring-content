package internal.org.springframework.content.s3.it;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.content.s3.config.EnableS3Stores;
import org.springframework.content.test.support.TestSupportPackages;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URISyntaxException;

@SpringBootApplication(scanBasePackages = {"internal.org.springframework.content.s3.it", TestSupportPackages.JPA})
@EntityScan(basePackages = TestSupportPackages.JPA)
@EnableJpaRepositories(basePackages = {TestSupportPackages.JPA, "internal.org.springframework.content.s3.it"},
    considerNestedRepositories = true)
@EnableS3Stores(basePackages = {TestSupportPackages.JPA, "internal.org.springframework.content.s3.it"})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Configuration
    public static class AppConfig {

        @Bean
        public S3Client amazonS3() throws URISyntaxException {
            return LocalStack.getAmazonS3Client();
        }
    }
}
