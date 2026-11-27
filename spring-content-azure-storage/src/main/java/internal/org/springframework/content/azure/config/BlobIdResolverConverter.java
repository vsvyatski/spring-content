package internal.org.springframework.content.azure.config;

import org.springframework.content.azure.Bucket;
import org.springframework.content.azure.config.BlobId;
import org.springframework.content.commons.convert.AbstractBucketBlobIdResolverConverter;

public class BlobIdResolverConverter extends AbstractBucketBlobIdResolverConverter {

    public BlobIdResolverConverter(String defaultBucket) {
        super(defaultBucket, Bucket.class, BlobId.class, BlobId::new);
    }
}
