package org.springframework.content.encryption;

import org.testcontainers.localstack.LocalStackContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Shared LocalStack Testcontainers instance for encryption module S3 ITs.
 *
 * @author Vladimir Svyatski
 */
public final class LocalStack {

    private static final LocalStackContainer CONTAINER = startLocalStack();

    private LocalStack() {
    }

    private static LocalStackContainer startLocalStack() {
        LocalStackContainer container = new LocalStackContainer("localstack/localstack").withServices("s3");
        container.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (container.isRunning()) {
                container.stop();
            }
        }));
        return container;
    }

    public static S3Client getAmazonS3Client() {
        return S3Client.builder()
            .endpointOverride(CONTAINER.getEndpoint())
            .region(Region.of(CONTAINER.getRegion()))
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                CONTAINER.getAccessKey(), CONTAINER.getSecretKey())))
            .serviceConfiguration(builder -> builder.pathStyleAccessEnabled(true).build())
            .build();
    }
}
