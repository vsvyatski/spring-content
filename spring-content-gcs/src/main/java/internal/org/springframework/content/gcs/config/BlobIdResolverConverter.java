package internal.org.springframework.content.gcs.config;

import com.google.cloud.storage.BlobId;
import org.springframework.content.commons.convert.AbstractBucketBlobIdResolverConverter;
import org.springframework.content.gcs.Bucket;

public class BlobIdResolverConverter extends AbstractBucketBlobIdResolverConverter {

    public BlobIdResolverConverter(String defaultBucket) {
        super(defaultBucket, Bucket.class, BlobId.class, BlobId::of);
    }
}
