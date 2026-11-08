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

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

public class LocalStack extends LocalStackContainer implements Serializable {

    private static final DockerImageName IMAGE_NAME = DockerImageName.parse("localstack/localstack");

    private LocalStack() {
        super(IMAGE_NAME);
        withServices(Service.S3);
        start();
    }

    private static final LocalStack INSTANCE = new LocalStack();

    public static S3Client getAmazonS3Client() throws URISyntaxException {
        return S3Client.builder()
                .endpointOverride(new URI(INSTANCE.getEndpointConfiguration().getServiceEndpoint()))
                .region(Region.US_EAST_1) // Set a region, so it does not need to be configured externally
                .credentialsProvider(new CrossAwsCredentialsProvider(INSTANCE.getDefaultCredentialsProvider()))
                .serviceConfiguration((builder) -> builder.pathStyleAccessEnabled(true).build())
                .build();
    }

    /**
     * This method was removed from {@link LocalStackContainer}. We'll keep a little modified version here.
     *
     * @return an {@link AwsClientBuilder.EndpointConfiguration}
     */
    private AwsClientBuilder.EndpointConfiguration getEndpointConfiguration() {
        return new AwsClientBuilder.EndpointConfiguration(getEndpointOverride(Service.S3).toString(), getRegion());
    }

    /**
     * This method was removed from {@link LocalStackContainer}, we'll just add it here for now.
     *
     * @return an {@link AWSCredentialsProvider}
     */
    private AWSCredentialsProvider getDefaultCredentialsProvider() {
        return new AWSStaticCredentialsProvider(new BasicAWSCredentials(getAccessKey(), getSecretKey()));
    }

    @Override
    public URI getEndpointOverride(EnabledService service) {
        try {
            // super method converts localhost to 127.0.0.1 which fails on macOS
            // need to revert it back to whatever getContainerIpAddress() returns
            return new URIBuilder(super.getEndpointOverride(service)).setHost(getHost()).build();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Cannot obtain endpoint URL", e);
        }
    }

    @SuppressWarnings("unused") // Serializable safe singleton usage
    protected LocalStack readResolve() {
        return INSTANCE;
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
