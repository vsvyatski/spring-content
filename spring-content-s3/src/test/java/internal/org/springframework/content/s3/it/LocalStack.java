package internal.org.springframework.content.s3.it;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.net.URIBuilder;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Shared LocalStack Testcontainers instance for S3 ITs.
 *
 * @author Vladimir Svyatski
 */
public final class LocalStack {

    private static final DockerImageName IMAGE_NAME = DockerImageName.parse("localstack/localstack");

    private static final LocalStackContainer CONTAINER = startLocalStack();

    private LocalStack() {
    }

    @SuppressWarnings("resource")
    private static LocalStackContainer startLocalStack() {
        LocalStackContainer container = new LocalStackContainer(IMAGE_NAME);
        container.withServices(LocalStackContainer.Service.S3);
        container.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (container.isRunning()) {
                container.stop();
            }
        }));
        return container;
    }

    public static S3Client getAmazonS3Client() throws URISyntaxException {
        return S3Client.builder()
            .endpointOverride(new URI(endpointConfiguration().getServiceEndpoint()))
            .region(Region.US_EAST_1)
            .credentialsProvider(new CrossAwsCredentialsProvider(defaultCredentialsProvider()))
            .serviceConfiguration(builder -> builder.pathStyleAccessEnabled(true).build())
            .build();
    }

    private static AwsClientBuilder.EndpointConfiguration endpointConfiguration() {
        return new AwsClientBuilder.EndpointConfiguration(s3EndpointUri().toString(), CONTAINER.getRegion());
    }

    private static AWSCredentialsProvider defaultCredentialsProvider() {
        return new AWSStaticCredentialsProvider(new BasicAWSCredentials(CONTAINER.getAccessKey(), CONTAINER.getSecretKey()));
    }

    private static URI s3EndpointUri() {
        try {
            return new URIBuilder(CONTAINER.getEndpointOverride(LocalStackContainer.Service.S3))
                .setHost(CONTAINER.getHost())
                .build();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Cannot obtain endpoint URL", e);
        }
    }

    private record CrossAwsCredentialsProvider(AWSCredentials credentials) implements AwsCredentialsProvider {
        private CrossAwsCredentialsProvider(AWSCredentialsProvider credentials) {
            this(credentials.getCredentials());
        }

        @Override
        public AwsCredentials resolveCredentials() {
            return AwsBasicCredentials.create(credentials.getAWSAccessKeyId(), credentials.getAWSSecretKey());
        }
    }
}