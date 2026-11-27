package internal.org.springframework.content.s3.boot.autoconfigure;

import internal.org.springframework.content.s3.config.S3StoreConfiguration;
import internal.org.springframework.content.s3.config.S3StoreFactoryBean;
import internal.org.springframework.versions.jpa.boot.autoconfigure.JpaVersionsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@AutoConfiguration
@AutoConfigureAfter({JpaVersionsAutoConfiguration.class})
@ConditionalOnClass({S3StoreConfiguration.class, S3Client.class})
@ConditionalOnProperty(prefix = "spring.content.storage.type", name = "default",
    havingValue = "s3", matchIfMissing = true)
public class S3ContentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean()
    public S3Client amazonS3(S3Properties props) {
        S3ClientBuilder builder = S3Client.builder();

        if (StringUtils.hasText(props.getEndpoint())) {
            builder.endpointOverride(URI.create(props.getEndpoint()));
        }

        if (StringUtils.hasText(props.getAccessKey()) && StringUtils.hasText(props.getSecretKey())) {
            AwsCredentialsProvider provider = StaticCredentialsProvider
                .create(AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey()));
            builder.credentialsProvider(provider);
        }

        if (props.isPathStyleAccess()) {
            builder.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        }

        return builder.build();
    }

    @Configuration
    @ConditionalOnMissingBean(S3StoreFactoryBean.class)
    @Import({S3StoreConfiguration.class, S3ContentAutoConfigureRegistrar.class})
    public static class EnableS3StoresConfig {
    }

    @Component
    @ConfigurationProperties(prefix = "spring.content.s3")
    public static class S3Properties {

        private String endpoint;
        private String accessKey;
        private String secretKey;
        private boolean pathStyleAccess;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public boolean isPathStyleAccess() {
            return pathStyleAccess;
        }

        public void setPathStyleAccess(boolean pathStyleAccess) {
            this.pathStyleAccess = pathStyleAccess;
        }
    }
}
